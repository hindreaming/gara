// Monitoring: health, the shared GitHub commit budget and the leases held right now.
import {$, cell, fillRows, row, time} from '../dom.js';

export function renderMonitoring(overview) {
    $('stat-health').textContent = overview.health;
    $('stat-health').className = `stat-value status-${overview.health}`;
    $('stat-budget').textContent = Math.max(0, Math.floor(overview.writeBudget.available));
    $('stat-burst').textContent = overview.writeBudget.burst;
    $('stat-rate').textContent = overview.writeBudget.perHour;
    $('stat-multipart').textContent = overview.pendingMultipartUploads;
    $('stat-database').textContent = overview.database;

    fillRows('health-rows', Object.entries(overview.healthChecks).map(([name, status]) => row([
        cell(name), cell(status, `status-${status}`),
    ])), 'No health checks');

    fillRows('lease-rows', overview.leases.map((l) => row([
        cell(l.name), cell(l.ownerId, 'muted'), cell(time(l.expiresAt)),
    ])), 'No leases held');
}
