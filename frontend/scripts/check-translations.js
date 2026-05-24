#!/usr/bin/env node
// node scripts/check-translations.js
// Reads all en/ JSON files, compares keys against bs/, sr/, hr/
// Prints missing keys with their English values
// Exit code 1 if anything is missing (useful in CI)

import { readdirSync, readFileSync } from 'fs'
import { join, basename } from 'path'
import { fileURLToPath } from 'url'
import { dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const LOCALES_DIR = join(__dirname, '..', 'public', 'locales')
const SOURCE_LANG = 'en'
const TARGET_LANGS = ['bs', 'sr', 'hr']

function readJson(path) {
  try {
    return JSON.parse(readFileSync(path, 'utf8'))
  } catch {
    return null
  }
}

function getAllKeys(obj, prefix = '') {
  return Object.entries(obj).flatMap(([k, v]) => {
    const fullKey = prefix ? `${prefix}.${k}` : k
    return typeof v === 'object' && v !== null ? getAllKeys(v, fullKey) : [fullKey]
  })
}

const sourceDir = join(LOCALES_DIR, SOURCE_LANG)
const namespaces = readdirSync(sourceDir).filter((f) => f.endsWith('.json'))

let totalMissing = 0

for (const ns of namespaces) {
  const sourceData = readJson(join(sourceDir, ns))
  if (!sourceData) continue
  const sourceKeys = getAllKeys(sourceData)

  for (const lang of TARGET_LANGS) {
    const targetPath = join(LOCALES_DIR, lang, ns)
    const targetData = readJson(targetPath)

    if (!targetData) {
      console.error(`\x1b[31m[MISSING FILE]\x1b[0m ${lang}/${ns}`)
      totalMissing += sourceKeys.length
      continue
    }

    const targetKeys = new Set(getAllKeys(targetData))
    const missing = sourceKeys.filter((k) => !targetKeys.has(k))

    if (missing.length > 0) {
      console.log(`\n\x1b[33m${lang}/${ns}\x1b[0m — ${missing.length} missing key(s):`)
      for (const key of missing) {
        const value = key.split('.').reduce((o, k) => o?.[k], sourceData)
        console.log(`  \x1b[90m${key}\x1b[0m: "${value}"`)
      }
      totalMissing += missing.length
    }
  }
}

if (totalMissing === 0) {
  console.log('\x1b[32m✓ All translation keys are present in bs, sr, hr.\x1b[0m')
  process.exit(0)
} else {
  console.log(`\n\x1b[31m✗ ${totalMissing} missing key(s) total.\x1b[0m`)
  process.exit(1)
}
