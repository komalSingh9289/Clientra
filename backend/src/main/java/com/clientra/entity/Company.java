package com.clientra.entity;

import jakarta.persistence.*;

/** tbl_companies: the business that owns the CRM data (synopsis 7.6 table 1). */
@Entity
@Table(name = "tbl_companies")
public class Company {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer companyId;
    @Column(nullable = false, length = 150)
    private String companyName;
    @Column(unique = true, length = 120)
    private String email;
    @Column(length = 120)
    private String phone;
    @Column(length = 150)
    private String website;
    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(length = 100)
    private String industry;

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
}
