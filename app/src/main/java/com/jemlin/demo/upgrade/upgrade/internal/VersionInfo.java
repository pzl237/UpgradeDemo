package com.jemlin.demo.upgrade.upgrade.internal;

/**
 * 版本信息对象
 *
 * @author panzhilong
 */
public class VersionInfo {

    /**
     * 例如100。安卓传versionCode，iOS传Build
     */
    private int versionCode;

    /**
     * 例如2.0.1。安卓传versionName，iOS传Version
     */
    private String version;

    /**
     * 下载链接
     */
    private String downloadUrl;

    /**
     * 包文件checksum
     */
    private String md5;

    /**
     * 是否强制升级
     */
    private boolean isMustUpgrade = false;
    /**
     * versionDesc : 版本说明。服务端注意，换行使用\n
     */

    private String versionDesc;

    public VersionInfo() {
    }

    public VersionInfo(int versionCode, String versionName,
                       String versionDesc, String downloadUrl, String md5, boolean isMustUpgrade) {
        this.versionCode = versionCode;
        this.version = versionName;
        this.versionDesc = versionDesc;
        this.downloadUrl = downloadUrl;
        this.md5 = md5;
        this.isMustUpgrade = isMustUpgrade;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(":: VERSION -> ");
        builder.append("VersionCode:").append(versionCode).append(", ");
        builder.append("VersionName:").append(version).append(", ");
        builder.append("versionDesc:").append(versionDesc).append(", ");
        builder.append("downloadUrl:").append(downloadUrl).append(", ");
        builder.append("md5:").append(md5).append(", ");
        builder.append("IsMustUpgrade:").append(isMustUpgrade);
        return builder.toString();
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public boolean isMustUpgrade() {
        return isMustUpgrade;
    }

    public void setMustUpgrade(boolean mustUpgrade) {
        isMustUpgrade = mustUpgrade;
    }

    public String getVersionDesc() {
        return versionDesc;
    }

    public void setVersionDesc(String versionDesc) {
        this.versionDesc = versionDesc;
    }
}
