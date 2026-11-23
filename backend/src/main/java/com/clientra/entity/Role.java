package com.clientra.entity;

import jakarta.persistence.*;

/** tbl_roles: Administrator, Sales Manager, Sales Representative (synopsis 3.10). */
@Entity
@Table(name = "tbl_roles")
public class Role {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer roleId;
    @Column(nullable = false, unique = true, length = 50)
    private String roleName;
    @Column(columnDefinition = "TEXT")
    private String description;

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
