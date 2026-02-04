const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
async function capture() {
    const args = process.argv.slice(2);
    if (args.length < 1) {
        console.error('Usage: node capture.js <url_or_file> [count] [interval_ms]');
        process.exit(1);
    }
    const input = args[0];
    const count = parseInt(args[1]) || 1;
    const interval = parseInt(args[2]) || 1000;
    let urls = [];
    try {
        if (fs.existsSync(input) && fs.lstatSync(input).isFile()) {
            console.log(`Reading URLs from file: ${input}`);
            const fileContent = fs.readFileSync(input, 'utf-8');
            urls = fileContent.split('\n').map(line => line.trim()).filter(line => line.length > 0);
        } else {
            urls = [input];
        }
    } catch (err) {
        // Fallback to treating input as a URL if file check fails (e.g. permission issue or invalid path syntax treated as URL)
        urls = [input];
    }
    if (urls.length === 0) {
        console.error('No URLs found to process.');
        process.exit(1);
    }
    const screenshotsDir = path.join(__dirname, 'screenshots');
    if (!fs.existsSync(screenshotsDir)) {
        fs.mkdirSync(screenshotsDir);
    }
    const browser = await chromium.launch();
    const context = await browser.newContext({
        viewport: { width: 1280, height: 720 }
    });
    const page = await context.newPage();
    for (const url of urls) {
        console.log(`\nProcessing URL: ${url}`);
        try {
            await page.goto(url, { waitUntil: 'networkidle' });
        } catch (e) {
            console.error(`Failed to navigate to ${url}: ${e.message}`);
            continue;
        }
        // Sanitize URL for filename
        const urlSanitized = url.replace(/^https?:\/\//, '').replace(/[^a-zA-Z0-9]/g, '-');
        for (let i = 0; i < count; i++) {
            const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
            const filename = path.join(screenshotsDir, `${urlSanitized}-${timestamp}-${i + 1}.png`);
            await page.screenshot({ path: filename, fullPage: true });
            console.log(`Saved: ${filename}`);
            if (i < count - 1) {
                await page.waitForTimeout(interval);
            }
        }
    }
    await browser.close();
    console.log('\nAll done.');
}
capture();