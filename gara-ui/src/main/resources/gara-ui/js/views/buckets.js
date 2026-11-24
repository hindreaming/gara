// Buckets: list, create and delete.
import {api} from '../api.js';
import {$, actionButton, bytes, cell, el, fillRows, note, row} from '../dom.js';

export function initBuckets(onChange) {
    const form = $('bucket-form');
    const toggle = (open) => {
        form.hidden = !open;
    };
    $('create-bucket-toggle').addEventListener('click', () => toggle(form.hidden));
    $('create-bucket-cancel').addEventListener('click', () => toggle(false));

    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const data = new FormData(form);
        const request = {
            name: data.get('name'),
            repository: data.get('repository'),
            directory: data.get('directory') || '',
            createRepository: data.get('createRepository') === 'on',
            publicRepos: data.get('publicRepos') === 'on',
        };
        try {
            await api.createBucket(request);
            note('bucket-note', `Created bucket ${request.name}`);
            form.reset();
            toggle(false);
        } catch (error) {
            note('bucket-note', `Create failed: ${error.message}`, true);
        }
        onChange();
    });

    async function remove(name) {
        if (!confirm(`Delete bucket ${name}? Its repositories stay on GitHub.`)) {
            return;
        }
        try {
            await api.deleteBucket(name);
            note('bucket-note', `Deleted bucket ${name}`);
        } catch (error) {
            note('bucket-note', `Delete failed: ${error.message}`, true);
        }
        onChange();
    }

    return function render(overview) {
        fillRows('bucket-rows', overview.buckets.map((b) => row([
            el('td', {}, [el('a', {className: 'name-cell', text: b.id, onClick: () => { location.hash = `#/browser/${encodeURIComponent(b.id)}/`; }})]),
            cell(b.repository),
            cell(b.directory || '(root)', b.directory ? '' : 'muted'),
            cell(b.publicRepos ? 'Public' : 'Private', b.publicRepos ? 'status-FULL' : 'muted'),
            cell(b.objects),
            cell(bytes(b.totalSize)),
            cell(b.activeVolumes),
            el('td', {}, [actionButton('Delete', () => remove(b.id), {
                disabled: b.objects > 0,
                title: b.objects > 0 ? 'Only empty buckets can be deleted' : 'Delete (its repositories stay on GitHub)',
            })]),
        ])), 'No buckets yet. Create one to start storing objects.');
    };
}
