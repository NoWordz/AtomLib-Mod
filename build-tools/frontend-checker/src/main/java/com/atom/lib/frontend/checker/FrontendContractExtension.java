package com.atom.lib.frontend.checker;

import java.io.File;

/** Explicit prototype inputs; reviewed config is supplied by the owning CI. */
public class FrontendContractExtension {
    private File contract, reviewed;
    private File reviewedRepository;
    private String reviewedRevision,reviewedConfig;
    public File getContract(){return contract;}
    public void setContract(File file){contract=file;}
    public File getReviewed(){return reviewed;}
    public void setReviewed(File file){reviewed=file;}
    public File getReviewedRepository(){return reviewedRepository;}
    public void setReviewedRepository(File file){reviewedRepository=file;}
    public String getReviewedRevision(){return reviewedRevision;}
    public void setReviewedRevision(String value){reviewedRevision=value;}
    public String getReviewedConfig(){return reviewedConfig;}
    public void setReviewedConfig(String value){reviewedConfig=value;}
}
