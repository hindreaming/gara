// Gara console: hash routing between views and a periodic overview refresh that feeds them.
import {api} from './api.js';
import {$, time} from './dom.js';
import {initBrowser} from './views/browser.js';
import {initBuckets} from './views/buckets.js';
import {renderMonitoring} from './views/monitoring.js';
import {renderVolumes} from './views/volumes.js';

const REFRESH_MS = 10000;
const TITLES = {browser: 'Object Browser', buckets: 'Buckets', volumes: 'Volumes', monitoring: 'Monitoring'};

const browser = initBrowser();
const renderBuckets = initBuckets(refresh);

function route() {
    const [view, ...rest] = location.hash.replace(/^#\/?/, '').split('/');
    const current = TITLES[view] ? view : 'browser';
    Object.keys(TITLES).forEach((name) => {
        $(`view-${name}`).hidden = name !== current;
    });
    document.querySelectorAll('.menu a').forEach((link) => {
        link.classList.toggle('active', link.dataset.view === current);
    });
    $('page-title').textContent = TITLES[current];
    if (current === 'browser') {
        browser.open(rest.join('/'));
    }
}

async function refresh() {
    const badge = $('health-badge');
    try {
        const overview = await api.overview();
        badge.textContent = overview.health;
        badge.className = `pill status-${overview.health}`;
        $('updated-at').textContent = `Updated ${time(overview.generatedAt)}`;
        browser.update(overview);
        renderBuckets(overview);
        renderVolumes(overview);
        renderMonitoring(overview);
    } catch (error) {
        badge.textContent = 'Unreachable';
        badge.className = 'pill status-DOWN';
        $('updated-at').textContent = error.message;
    }
}

window.addEventListener('hashchange', route);
route();
refresh();
setInterval(refresh, REFRESH_MS);
