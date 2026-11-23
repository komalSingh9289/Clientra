// Shared constants. Role names match the synopsis (Section 3.10 / 11.2).
export const ROLES = { ADMIN: "Administrator", MANAGER: "Sales Manager", REP: "Sales Representative" };
export const ACTIVITY_TYPES = ["Call", "Email", "Meeting", "Note"]; // tbl_activities.type ENUM
export const LEAD_SOURCES = ["Website", "Referral", "LinkedIn", "Cold Call", "Trade Show", "Email Campaign"];
export const REMINDER_STATUS = ["Pending", "Completed", "Cancelled"]; // tbl_reminders.status ENUM
export const DEAL_STATUS = ["Open", "Won", "Lost"]; // tbl_deals.status ENUM
export const TONES = ["Friendly", "Professional", "Concise"];
