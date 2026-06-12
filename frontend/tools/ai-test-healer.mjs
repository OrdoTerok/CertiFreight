import fs from 'node:fs';
import path from 'node:path';

const args = new Set(process.argv.slice(2));
const analyzeFrontend = args.has('--frontend') || args.size === 0;

const cwd = process.cwd();

const findPlaywrightErrorHints = (text) => {
    const hints = [];
    if (text.includes('strict mode violation')) {
        hints.push('Selector matched multiple nodes. Prefer getByRole/getByLabel with exact name filters.');
    }
    if (text.includes('Timeout') || text.includes('timed out')) {
        hints.push('Potential UI drift or race. Add adaptive locator fallback or wait for visible state.');
    }
    if (text.includes('toHaveURL')) {
        hints.push('Routing assertion failed. Consider regex URL checks for transitional redirects.');
    }
    return hints;
};

if (analyzeFrontend) {
    const reportPath = path.join(cwd, 'test-results');
    if (!fs.existsSync(reportPath)) {
        console.log('[ai-test-healer] No test-results directory found. Run Playwright first.');
        process.exit(0);
    }

    const files = fs.readdirSync(reportPath, { withFileTypes: true });
    const errorLogs = [];

    for (const entry of files) {
        if (!entry.isDirectory()) {
            continue;
        }
        const errorContext = path.join(reportPath, entry.name, 'error-context.md');
        if (fs.existsSync(errorContext)) {
            errorLogs.push(errorContext);
        }
    }

    if (errorLogs.length === 0) {
        console.log('[ai-test-healer] No failure logs found. Great run.');
        process.exit(0);
    }

    console.log(`[ai-test-healer] Found ${errorLogs.length} failure log(s).`);
    for (const file of errorLogs) {
        const text = fs.readFileSync(file, 'utf8');
        const hints = findPlaywrightErrorHints(text);
        console.log(`\n- ${file}`);
        if (hints.length === 0) {
            console.log('  Suggestion: inspect screenshot + trace; add resilient locator and remove hard-coded timing.');
            continue;
        }
        for (const hint of hints) {
            console.log(`  Suggestion: ${hint}`);
        }
    }
}

