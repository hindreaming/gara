// Object Browser: the bucket list, then one folder level at a time. Location lives in the hash:
// #/browser, or #/browser/<bucket>/<prefix>.
import {api} from '../api.js';
import {$, actionButton, bytes, cell, el, fillRows, icon, note, row, time} from '../dom.js';

const state = {bucket: null, prefix: '', buckets: []};

function go(bucket, prefix = '') {
    location.hash = bucket ? `#/browser/${encodeURIComponent(bucket)}/${prefix}` : '#/browser';
}

function nameCell(iconName, text, onClick) {
    return el('td', {}, [el('a', {className: `name-cell ${iconName}`, onClick}, [icon(iconName), text])]);
}

function head(...titles) {
    $('browser-head').replaceChildren(row(titles.map((t) => el('th', {text: t}))));
}

function renderBreadcrumb() {
    const crumbs = [el('a', {text: 'Buckets', onClick: () => go(null)})];
    if (state.bucket) {
        crumbs.push('/', el('a', {text: state.bucket, onClick: () => go(state.bucket)}));
        let path = '';
        state.prefix.split('/').filter(Boolean).forEach((part) => {
            path += `${part}/`;
            const target = path;
            crumbs.push('/', el('a', {text: part, onClick: () => go(state.bucket, target)}));
        });
    }
    $('breadcrumb').replaceChildren(...crumbs);
    $('upload-button').hidden = !state.bucket;
}

function renderBucketList() {
    head('Name', 'Objects', 'Size', 'Access');
    fillRows('browser-rows', state.buckets.map((b) => row([
        nameCell('bucket', b.id, () => go(b.id)),
        cell(b.objects),
        cell(bytes(b.totalSize)),
        cell(b.publicRepos ? 'Public' : 'Private', 'muted'),
    ])), 'No buckets yet. Create one under Buckets.');
}

async function renderFolder() {
    head('Name', 'Last modified', 'Size', '');
    let listing;
    try {
        listing = await api.browse(state.bucket, state.prefix);
    } catch (error) {
        fillRows('browser-rows', [], `Could not list objects: ${error.message}`);
        return;
    }
    const folders = listing.folders.map((folder) => row([
        nameCell('folder', folder.substring(state.prefix.length), () => go(state.bucket, folder)),
        cell('–', 'muted'), cell('–', 'muted'), cell(''),
    ]));
    const objects = listing.objects.map((object) => row([
        nameCell('file', object.name, () => window.open(api.downloadUrl(state.bucket, object.key))),
        cell(time(object.lastModified)),
        cell(bytes(object.size)),
        el('td', {}, [
            actionButton('Download', () => window.open(api.downloadUrl(state.bucket, object.key))),
            actionButton('Delete', () => removeObject(object.key)),
        ]),
    ]));
    fillRows('browser-rows', [...folders, ...objects], 'This folder is empty. Upload a file to get started.');
    note('browser-note', listing.truncated ? 'Showing the first 1000 entries.' : '');
}

async function removeObject(key) {
    if (!confirm(`Delete ${key}? It stays in the repository's git history.`)) {
        return;
    }
    try {
        await api.deleteObject(state.bucket, key);
        note('browser-note', `Deleted ${key}`);
    } catch (error) {
        note('browser-note', `Delete failed: ${error.message}`, true);
    }
    renderFolder();
}

async function uploadFiles(files) {
    for (const file of files) {
        note('browser-note', `Uploading ${file.name}…`);
        try {
            await api.upload(state.bucket, state.prefix, file);
            note('browser-note', `Uploaded ${file.name}`);
        } catch (error) {
            note('browser-note', `Upload of ${file.name} failed: ${error.message}`, true);
            break;
        }
    }
    renderFolder();
}

function render() {
    renderBreadcrumb();
    if (state.bucket) {
        renderFolder();
    } else {
        renderBucketList();
    }
}

export function initBrowser() {
    $('upload-input').addEventListener('change', (event) => {
        uploadFiles([...event.target.files]);
        event.target.value = '';
    });
    $('refresh-objects').addEventListener('click', render);

    return {
        // Called with the hash path after "#/browser/": "<bucket>/<prefix>" or "".
        open(path) {
            const slash = path.indexOf('/');
            state.bucket = path ? decodeURIComponent(slash < 0 ? path : path.substring(0, slash)) : null;
            state.prefix = slash < 0 ? '' : path.substring(slash + 1);
            note('browser-note', '');
            render();
        },
        update(overview) {
            state.buckets = overview.buckets;
            if (!state.bucket) {
                renderBucketList();
            }
        },
    };
}
