package com.atom.lib.frontend.contract;

import java.util.*;
import org.junit.jupiter.api.Test;
import static com.atom.lib.frontend.contract.ContractDesign.*;
import static org.junit.jupiter.api.Assertions.*;

class ContractCodecTest {
    @Test void preBindingWireFormatIsNotSilentlyReinterpreted(){byte[] bytes=ContractCodec.encode(DesignResolverTest.fixture());bytes[3]=2;assertThrows(IllegalArgumentException.class,()->ContractCodec.decode(bytes));}
    @Test void canonicalDataSurvivesRuntimeDecodeAndMapOrder(){var d=DesignResolverTest.fixture();byte[] bytes=ContractCodec.encode(d);
        assertEquals(d,ContractCodec.decode(bytes));
        var reversed=new LinkedHashMap<String,Family>();new TreeMap<>(d.families()).descendingMap().forEach(reversed::put);
        assertArrayEquals(bytes,ContractCodec.encode(DesignResolverTest.copy(d,reversed,d.roles(),d.recipes(),d.pages())));}
    @Test void truncatedTrailingOrChangedFormatCannotBeConsumed(){byte[] good=ContractCodec.encode(DesignResolverTest.fixture());assertTrue(good.length>10,"canonical package must contain actual design");
        assertThrows(IllegalArgumentException.class,()->ContractCodec.decode(Arrays.copyOf(good,good.length-1)));
        assertThrows(IllegalArgumentException.class,()->ContractCodec.decode(Arrays.copyOf(good,good.length+1)));
        good[0]^=1;assertThrows(IllegalArgumentException.class,()->ContractCodec.decode(good));}
    @Test void decodedCollectionsAreImmutableAndDesignsDoNotShareState(){var a=ContractCodec.decode(ContractCodec.encode(DesignResolverTest.fixture()));
        assertThrows(UnsupportedOperationException.class,()->a.families().clear());assertThrows(UnsupportedOperationException.class,()->a.recipes().get("card").bindings().clear());}
}
