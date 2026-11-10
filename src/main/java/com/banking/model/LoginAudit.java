package com.banking.model;

import com.banking.enums.LoginStatus;

import java.time.LocalDateTime;

/**
 * Audit domain entity recording all customer authentication events.
 */
public class LoginAudit {

    private Long loginAuditId;
    private Long customerId;
    private String username;
    private LoginStatus loginStatus;
    private String ipAddress;
    private String deviceInfo;
    private String failureReason;
    private LocalDateTime loginTime;

    public LoginAudit() {
        this.loginTime = LocalDateTime.now();
    }

    public LoginAudit(Long customerId, String username, LoginStatus loginStatus,
                      String ipAddress, String deviceInfo, String failureReason) {
        this.customerId = customerId;
        this.username = username;
        this.loginStatus = loginStatus;
        this.ipAddress = ipAddress;
        this.deviceInfo = deviceInfo;
        this.failureReason = failureReason;
        this.loginTime = LocalDateTime.now();
    }

    public Long getLoginAuditId() {
        return loginAuditId;
    }

    public void setLoginAuditId(Long loginAuditId) {
        this.loginAuditId = loginAuditId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LoginStatus getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(LoginStatus loginStatus) {
        this.loginStatus = loginStatus;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    @Override
    public String toString() {
        return "LoginAudit{" +
                "id=" + loginAuditId +
                ", username='" + username + '\'' +
                ", status=" + loginStatus +
                ", time=" + loginTime +
                (failureReason != null ? ", reason='" + failureReason + '\'' : "") +
                '}';
    }
}
