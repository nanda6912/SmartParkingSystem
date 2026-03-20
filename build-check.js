#!/usr/bin/env node

/**
 * Build-time security check
 * Prevents demo authentication files from being included in production builds
 */

const fs = require('fs');
const path = require('path');

// Files and patterns that should NOT be in production
const DEMO_FILES = [
    'src/main/resources/static/auth-demo.html',
    'src/main/resources/static/demo-auth.html',
    'src/main/resources/static/demo-login.html'
];

// File patterns to detect potential demo authentication files
const DEMO_PATTERNS = [
    /auth-demo\.html$/i,
    /demo-auth\.html$/i,
    /demo-login\.html$/i,
    /.*demo.*auth.*\.html$/i,
    /.*auth.*demo.*\.html$/i
];

// Check if we're in production mode
const isProduction = process.env.NODE_ENV === 'production';

if (isProduction) {
    console.log('🔒 Running production security checks...');
    
    // Compute project root from script location
    const projectRoot = path.resolve(__dirname, '.');
    
    let hasViolations = false;
    
    // Check specific demo files
    DEMO_FILES.forEach(file => {
        const filePath = path.resolve(projectRoot, file);
        
        if (fs.existsSync(filePath)) {
            console.error(`❌ SECURITY VIOLATION: Demo file found in production: ${file}`);
            console.error(`   Path: ${filePath}`);
            console.error('   Demo authentication files must not be present in production builds!');
            hasViolations = true;
        }
    });
    
    // Check for demo files using pattern detection
    function scanDirectory(dir, patterns) {
        try {
            const files = fs.readdirSync(dir);
            files.forEach(file => {
                const filePath = path.join(dir, file);
                const stat = fs.statSync(filePath);
                
                if (stat.isDirectory()) {
                    // Recursively scan subdirectories (avoid node_modules)
                    if (file !== 'node_modules' && file !== 'target') {
                        scanDirectory(filePath, patterns);
                    }
                } else {
                    // Check if file matches any demo patterns
                    patterns.forEach(pattern => {
                        if (pattern.test(file)) {
                            console.error(`❌ SECURITY VIOLATION: Demo file detected by pattern: ${file}`);
                            console.error(`   Path: ${filePath}`);
                            console.error(`   Matched pattern: ${pattern}`);
                            console.error('   Demo authentication files must not be present in production builds!');
                            hasViolations = true;
                        }
                    });
                }
            });
        } catch (error) {
            // Ignore directories we can't read
        }
    }
    
    // Scan the static resources directory for demo files
    const staticDir = path.join(projectRoot, 'src/main/resources/static');
    if (fs.existsSync(staticDir)) {
        scanDirectory(staticDir, DEMO_PATTERNS);
    }
    
    if (hasViolations) {
        console.error('\n💥 Build failed due to security violations!');
        console.error('   Please remove demo authentication files before deploying to production.');
        process.exit(1);
    } else {
        console.log('✅ Production security checks passed!');
    }
} else {
    console.log('🛠️ Development mode - skipping production security checks');
}
