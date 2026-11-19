"""Append one species object to the END of a type's species list, as text in the file's own style.

Re-dumping the whole JSON with json.dump reformats every line (a 600-line diff for one entry), so
this inserts a block before the `]` that closes "species", copying the indentation and the
`"key":value` / `"key": value` spacing the file already uses, and keeps its newline style.

Appending (never inserting mid-list) is the only safe way to add a species: variants are saved as
integer indices, so anything else reassigns animals already in a world.

    python scripts/append_species.py <type> '<species json object>'
"""
import json
import re
import sys

ROOT = 'src/main/resources/data/untamedwilds/entities/'


def render(obj, indent, step, sep):
    pad = ' ' * indent
    inner = ' ' * (indent + step)
    if isinstance(obj, dict):
        items = ['%s"%s":%s%s' % (inner, k, sep, render(v, indent + step, step, sep).lstrip()) for k, v in obj.items()]
        return pad + '{\n' + ',\n'.join(items) + '\n' + pad + '}'
    if isinstance(obj, list):
        items = [render(v, indent + step, step, sep) for v in obj]
        return pad + '[\n' + ',\n'.join(items) + '\n' + pad + ']'
    return pad + json.dumps(obj, ensure_ascii=False)


def main():
    t, obj = sys.argv[1], json.loads(sys.argv[2])
    path = ROOT + t + '.json'
    raw = open(path, encoding='utf8').read()
    nl = '\r\n' if '\r\n' in raw else '\n'
    s = raw.replace('\r\n', '\n')
    d = json.loads(s)
    names = [sp['name'] for sp in d['species']]
    if obj['name'] in names:
        raise SystemExit('species %s already exists in %s' % (obj['name'], t))
    want = len(d['species'])
    if obj.get('variant') != want or d['species'][-1]['variant'] != want - 1:
        raise SystemExit('variant must be %d (next free index); got %s' % (want, obj.get('variant')))
    sep = ' ' if re.search(r'"name": "', s) else ''
    m = re.search(r'\n( *)"species" ?: ?\[\n( *)\{', s)
    step = len(m.group(2)) - len(m.group(1))
    obj_indent = len(m.group(2))
    # the `]` closing species is the first line, after the species key, indented like the key
    close = re.compile(r'\n%s\]' % m.group(1))
    c = close.search(s, m.end())
    block = ',\n' + render(obj, obj_indent, step, sep)
    # the previous object's closing brace sits right before this `]`
    s = s[:c.start()] + block + s[c.start():]
    d2 = json.loads(s)
    assert [sp['name'] for sp in d2['species']] == names + [obj['name']]
    open(path, 'w', encoding='utf8', newline='').write(s.replace('\n', nl))
    print('appended %s:%s as variant %d' % (t, obj['name'], want))


if __name__ == '__main__':
    main()
