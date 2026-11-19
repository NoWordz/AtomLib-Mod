"""Prove the preserved legacy behavior and fresh consumer compiler warning."""
from pathlib import Path
import argparse
import os
import subprocess
import tempfile

parser = argparse.ArgumentParser(description="Run two independently compiled 0.4 consumers on 0.6")
parser.add_argument("--java-home", required=True, type=Path)
parser.add_argument("--old", required=True, type=Path, help="An unmodified AtomLib 0.4.0 JAR")
parser.add_argument("--new", required=True, type=Path, help="The AtomLib 0.6.0 UI or platform JAR")
parser.add_argument("--skija", required=True, type=Path, help="The skija-shared JAR used by 0.6.0")
args = parser.parse_args()
jdk = args.java_home / "bin"
old, new, skija = (path.resolve(strict=True) for path in (args.old, args.new, args.skija))
evidence = Path("build/frontend-contract-lock")
evidence.mkdir(parents=True, exist_ok=True)
output = Path(tempfile.mkdtemp(prefix="two-legacy-jars-", dir=evidence)).resolve()

def invocation(*args, **kwargs):
    return subprocess.run([str(arg) for arg in args], check=True, **kwargs)

for name, color in (("FirstShop", "0xFFCC3344"), ("SecondShop", "0xFF2244EE")):
    source = output / f"{name}.java"
    classes = output / f"{name}-classes"
    classes.mkdir(exist_ok=True)
    source.write_text(f"""
import com.atom.lib.design.atomchat.*;
public final class {name} {{
    public static int ripples;
    public static void open() {{
        Design.host(new DesignHost() {{
            public int accentColor() {{ return {color}; }}
            public int textSecondaryColor() {{ return 0xFFCCCCCC; }}
            public int cardColor() {{ return -1; }}
            public int panelBackgroundColor() {{ return 0xFF141617; }}
            public float cardTint() {{ return 0.25f; }}
            public float cornerRadius() {{ return 20f; }}
            public boolean halfScreenMode() {{ return false; }}
        }});
        Design.rippleSink((x,y,w,h,r,c) -> ripples++);
    }}
    public static int accent() {{ return UiTokens.accentFill(); }}
    public static void click() {{ Design.ripple(0,0,1,1,0,0); }}
}}
""", encoding="utf-8")
    invocation(jdk/"javac", "--release", 17, "-cp", old, "-d", classes, source)
    invocation(jdk/"jar", "--create", "--file", output/f"{name}.jar", "-C", classes, ".")

driver = output / "TwoLegacyJars.java"
driver.write_text("""
import com.atom.lib.design.atomchat.*;
public final class TwoLegacyJars {
    public static void main(String[] args) {
        var clean = Design.defaultHost();
        FirstShop.open();
        int initial = FirstShop.accent();
        SecondShop.open();
        int after = FirstShop.accent();
        FirstShop.click();
        if (initial != 0xFFCC3344 || after != 0xFF2244EE || SecondShop.accent() != after)
            throw new AssertionError("Old theme behavior changed");
        if (FirstShop.ripples != 0 || SecondShop.ripples != 1)
            throw new AssertionError("Old ripple behavior changed");
        FirstShop.open();
        SecondShop.click();
        if (SecondShop.accent() != initial || FirstShop.ripples != 1 || SecondShop.ripples != 1)
            throw new AssertionError("Legacy behavior is order dependent");
        if (UiTokens.accentFill(clean) != 0xFF4A90E2 || UiTokens.accentFill(Design.defaultHost()) != 0xFF4A90E2)
            throw new AssertionError("Legacy writes changed isolated defaults");
        System.out.printf("first before=%08X after=%08X; isolated=%08X; both writes affect the other jar's next ripple%n",
            initial, after, UiTokens.accentFill(clean));
    }
}
""", encoding="utf-8")
result = invocation(jdk/"java", "-cp", os.pathsep.join(map(str,(new,skija,output/"FirstShop.jar",output/"SecondShop.jar"))), driver,
                    stdout=subprocess.PIPE, text=True)
print(result.stdout, end="")
(output / "results.txt").write_text(result.stdout, encoding="utf-8")




# A fresh consumer sees the diagnostic even if it never registers a global host.
fresh = output / "FreshConsumer.java"
def compile_consumer(theme_argument):
    fresh.write_text(
        "import com.atom.lib.design.atomchat.*;\n"
        "final class FreshConsumer { static int accent() { "
        f"return UiTokens.accentFill({theme_argument}); }} }}",
        encoding="utf-8",
    )
    return subprocess.run(
        [jdk / "javac", "-J-Duser.language=en", "--release", "17", "-Xlint:deprecation", "-Werror",
         "-cp", os.pathsep.join(map(str, (new, skija))), "-d", output, fresh],
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True,
    )

implicit = compile_consumer("")
if implicit.returncode == 0 or "accentFill()" not in implicit.stdout or "[deprecation]" not in implicit.stdout:
    raise AssertionError(implicit.stdout)
explicit = compile_consumer("Design.defaultHost()")
if explicit.returncode != 0:
    raise AssertionError(explicit.stdout)
(output / "implicit-compile.log").write_text(implicit.stdout, encoding="utf-8")
print(f"Legacy bytecode and fresh-consumer deprecation checks passed. Evidence: {output}")
