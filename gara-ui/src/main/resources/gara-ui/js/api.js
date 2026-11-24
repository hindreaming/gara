// The admin JSON API. The browser takes care of the HTTP Basic login.
const API = '/admin/api';

async function json(response) {
    if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(body.message || `${response.status} ${response.statusText}`);
    }
    return response.status === 204 ? null : response.json();
}

function objectsUrl(bucket) {
    return `${API}/buckets/${encodeURIComponent(bucket)}/objects`;
}

export const api = {
    overview: () => fetch(`${API}/overview`, {headers: {Accept: 'application/json'}}).then(json),

    createBucket: (request) => fetch(`${API}/buckets`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(request),
    }).then(json),

    deleteBucket: (name) => fetch(`${API}/buckets/${encodeURIComponent(name)}`, {method: 'DELETE'}).then(json),

    browse: (bucket, prefix) => fetch(`${objectsUrl(bucket)}?prefix=${encodeURIComponent(prefix)}`).then(json),

    upload: (bucket, prefix, file) => {
        const data = new FormData();
        data.append('file', file);
        data.append('prefix', prefix);
        return fetch(objectsUrl(bucket), {method: 'POST', body: data}).then(json);
    },

    deleteObject: (bucket, key) =>
        fetch(`${objectsUrl(bucket)}?key=${encodeURIComponent(key)}`, {method: 'DELETE'}).then(json),

    downloadUrl: (bucket, key) => `${objectsUrl(bucket)}/content?key=${encodeURIComponent(key)}`,
};
