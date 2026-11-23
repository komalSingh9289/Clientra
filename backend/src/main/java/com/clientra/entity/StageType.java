package com.clientra.entity;
/**
 * Tells the system what a pipeline stage MEANS, independent of its (renamable) name:
 * OPEN = work in progress, WON / LOST = closed. Dashboard, KPIs and the "active lead" count use this.
 */
public enum StageType { OPEN, WON, LOST }
