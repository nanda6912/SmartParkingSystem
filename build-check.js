#!/usr/bin/env node

/**
 * Build-time security check
 * Prevents demo authentication files from being included in production builds
 */

const fs = require('fs');
const path = require('path');

// Files that should NOT be in production
const DEMO_FILES = [
    // No demo files - auth-demo.html has been removed
];

// Check if we're in production mode
const isProduction = process.env.NODE_ENV === 'production';

if (isProduction) {
    console.log('🔒 Running production security checks...');
    
    // Compute project root from script location
    const projectRoot = path.resolve(__dirname, '.');
    
    let hasViolations = false;
    
    DEMO_FILES.forEach(file => {
        const filePath = path.resolve(projectRoot, file);
        
        if (fs.existsSync(filePath)) {
            console.error(`❌ SECURITY VIOLATION: Demo file found in production: ${file}`);
            console.error(`   Path: ${filePath}`);
            console.error('   Demo authentication files must not be present in production builds!');
            hasViolations = true;
        }
    });
    
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
