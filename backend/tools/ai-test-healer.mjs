import fs from 'node:fs';
import path from 'node:path';

const reportDir = path.join(process.cwd(), 'target', 'surefire-reports');

if (!fs.existsSync(reportDir)) {
    console.log('[backend-ai-healer] No surefire reports found. Run backend tests first.');
    process.exit(0);
}

const files = fs.readdirSync(reportDir).filter((file) => file.endsWith('.txt'));
const failures = [];

for (const file of files) {
    const fullPath = path.join(reportDir, file);
    const text = fs.readFileSync(fullPath, 'utf8');
    const hasFailure = text.includes('FAILURE!') || text.includes('<<< FAILURE!') || text.includes('<<< ERROR!');
    if (!hasFailure) {
        continue;
    }

    const hints = [];
    if (text.includes('expected:<') || text.includes('org.opentest4j.AssertionFailedError')) {
        hints.push('Assertion drift detected. Compare response payload contracts and update AI case catalog if behavior changed intentionally.');
    }
    if (text.includes('Connection refused') || text.includes('timed out')) {
        hints.push('Likely transient infrastructure issue. Self-healing rerun is enabled (Surefire rerunFailingTestsCount=1).');
    }
    if (text.includes('Constraint Violation')) {
        hints.push('Validation contract changed. Regenerate request scenarios in AiShipmentRequestCases.');
    }

    failures.push({ file: fullPath, hints });
}

if (failures.length === 0) {
    console.log('[backend-ai-healer] No failed reports detected.');
    process.exit(0);
}

console.log(`[backend-ai-healer] Found ${failures.length} failed report(s).`);
for (const failure of failures) {
    console.log(`\n- ${failure.file}`);
    if (failure.hints.length === 0) {
        console.log('  Suggestion: inspect stack trace and add/update a deterministic assertion boundary.');
        continue;
    }
    for (const hint of failure.hints) {
        console.log(`  Suggestion: ${hint}`);
    }
}

