package org.example.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatasetRecord {
    private String release;
    private String className;

    private int loc, wmc, dit, cbo, rfc, lcom;
    private int locTouched, age, aoc, nFix, fix, nf;
    private double avc, ala, acss, entropy;
    private int nSmells;
    private int bugginess;
    private int nr;
    private Set<String> authors;
    private int churnTotal;
    private int maxChurn;

    public LocalDateTime firstCommitDate = null;
    public LocalDateTime lastCommitDate = null;
    public int revs = 0;
    public int added = 0;
    public int churn = 0;
    public List<Integer> touchedPerCommit = new ArrayList<>();
    public List<Integer> changeSets = new ArrayList<>();

    public DatasetRecord(String release, String className) {
        this.release = release;
        this.className = className;
        this.authors = new HashSet<>();
    }

    public String getRelease() { return release; }
    public String getClassName() { return className; }

    public int getLoc() { return loc; }
    public void setLoc(int loc) { this.loc = loc; }

    public int getLocTouched() { return locTouched; }
    public void setLocTouched(int locTouched) { this.locTouched = locTouched; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public int getAoc() { return aoc; }
    public void setAoc(int aoc) { this.aoc = aoc; }

    public int getNFix() { return nFix; }
    public void setNFix(int nFix) { this.nFix = nFix; }

    public int getFix() { return fix; }
    public void setFix(int fix) { this.fix = fix; }

    public int getNf() { return nf; }
    public void setNf(int nf) { this.nf = nf; }

    public double getAvc() { return avc; }
    public void setAvc(double avc) { this.avc = avc; }

    public double getAla() { return ala; }
    public void setAla(double ala) { this.ala = ala; }

    public double getAcss() { return acss; }
    public void setAcss(double acss) { this.acss = acss; }

    public double getEntropy() { return entropy; }
    public void setEntropy(double entropy) { this.entropy = entropy; }

    public int getWmc() { return wmc; }
    public void setWmc(int wmc) { this.wmc = wmc; }

    public int getDit() { return dit; }
    public void setDit(int dit) { this.dit = dit; }

    public int getCbo() { return cbo; }
    public void setCbo(int cbo) { this.cbo = cbo; }

    public int getRfc() { return rfc; }
    public void setRfc(int rfc) { this.rfc = rfc; }

    public int getLcom() { return lcom; }
    public void setLcom(int lcom) { this.lcom = lcom; }

    public int getNSmells() { return nSmells; }
    public void setNSmells(int nSmells) { this.nSmells = nSmells; }

    public int getBugginess() { return bugginess; }
    public void setBugginess(int bugginess) { this.bugginess = bugginess; }

    public int getNr() { return nr; }
    public void setNr(int nr) { this.nr = nr; }

    public Set<String> getAuthors() { return authors; }

    public int getChurnTotal() { return churnTotal; }
    public void setChurnTotal(int churnTotal) { this.churnTotal = churnTotal; }

    public int getMaxChurn() { return maxChurn; }
    public void setMaxChurn(int maxChurn) { this.maxChurn = maxChurn; }
}