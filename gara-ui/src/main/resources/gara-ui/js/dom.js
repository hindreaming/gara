// Small DOM and formatting helpers shared by the views.
export const $ = (id) => document.getElementById(id);

export function bytes(value) {
    const units = ['B', 'KiB', 'MiB', 'GiB', 'TiB'];
    let size = value;
    let unit = 0;
    while (size >= 1024 && unit < units.length - 1) {
        size /= 1024;
        unit++;
    }
    return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

export function time(value) {
    return value ? new Date(value).toLocaleString() : '–';
}

export function el(tag, {className, text, title, onClick} = {}, children = []) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    if (title) node.title = title;
    if (onClick) node.addEventListener('click', onClick);
    node.append(...children);
    return node;
}

export const cell = (text, className) => el('td', {text: String(text), className});

export const row = (cells) => el('tr', {}, cells);

export function fillRows(tbodyId, rows, emptyText) {
    const tbody = $(tbodyId);
    if (rows.length > 0) {
        tbody.replaceChildren(...rows);
        return;
    }
    const empty = cell(emptyText, 'empty');
    empty.colSpan = 20;
    tbody.replaceChildren(row([empty]));
}

export function note(id, text, isError = false) {
    const target = $(id);
    target.textContent = text;
    target.classList.toggle('error', isError);
}

const ICONS = {
    bucket: 'M4 6c0-1.7 3.6-3 8-3s8 1.3 8 3v12c0 1.7-3.6 3-8 3s-8-1.3-8-3zM4 6c0 1.7 3.6 3 8 3s8-1.3 8-3',
    folder: 'M3 6h7l2 2h9v11H3z',
    file: 'M6 3h8l4 4v14H6zM14 3v4h4',
};

export function icon(name) {
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svg.setAttribute('viewBox', '0 0 24 24');
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    path.setAttribute('d', ICONS[name]);
    svg.appendChild(path);
    return svg;
}

export function actionButton(text, onClick, {disabled = false, title} = {}) {
    const button = el('button', {className: 'action', text, title, onClick});
    button.disabled = disabled;
    return button;
}
