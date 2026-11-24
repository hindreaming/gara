// Volumes: every GitHub repository Gara writes to.
import {bytes, cell, el, fillRows, row} from '../dom.js';

function usage(percent) {
    const bar = el('span', {className: 'bar'}, [el('span')]);
    bar.firstChild.style.width = `${Math.min(100, percent)}%`;
    return el('td', {}, [bar, `${percent}%`]);
}

export function renderVolumes(overview) {
    fillRows('volume-rows', overview.volumes.map((v) => row([
        cell(v.directory ? `${v.repoName}/${v.directory}` : v.repoName),
        cell(v.bucketId),
        cell(v.status, `status-${v.status}`),
        cell(bytes(v.writtenBytes)),
        usage(v.fillPercent),
        cell(v.headFiles),
        cell(v.busy ? 'Writing' : 'Idle', 'muted'),
    ])), 'No volumes yet');
}
