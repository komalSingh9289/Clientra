package com.clientra.service;

import com.clientra.entity.Lead;
import com.clientra.exception.ApiException;
import com.clientra.repository.LeadRepository;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * DATA-SCOPE rule in one place: a Sales Representative can only touch leads assigned to them;
 * Sales Managers and Administrators can touch every lead. Every service that receives a leadId
 * calls require(leadId) first, so the rule cannot be forgotten.
 */
@Component
public class LeadAccess {
    private final LeadRepository leads;
    private final CurrentUser current;

    public LeadAccess(LeadRepository leads, CurrentUser current) {
        this.leads = leads;
        this.current = current;
    }

    /** Loads the lead or throws 404; throws 403 if the caller is a rep who does not own it. */
    public Lead require(Integer leadId) {
        Lead l = leads.findById(leadId).orElseThrow(() -> ApiException.notFound("Lead not found."));
        check(l);
        return l;
    }

    public void check(Lead l) {
        AuthUser u = current.get();
        if (u.isRep() && !Objects.equals(l.getAssignedTo(), u.userId())) {
            throw ApiException.forbidden("You can only access leads assigned to you.");
        }
    }
}
