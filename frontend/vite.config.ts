import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from "@tailwindcss/vite";
import istanbul from 'vite-plugin-istanbul';

const plugins = [
    tailwindcss(),
    react(),
    ...(process.env.E2E_COVERAGE === 'true' ? [istanbul({
        include: 'src/**/*',
        exclude: ['node_modules', 'e2e', 'src/test', '**/*.test.ts', '**/*.test.tsx'],
        extension: ['.ts', '.tsx'],
        requireEnv: false,
    })] : []),
];

// https://vite.dev/config/
export default defineConfig({
    plugins,
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
        exclude: ['e2e/**', 'node_modules/**'],
        coverage: {
            provider: 'v8',
            reporter: ['text', 'json', 'html'],
            include: ['src/**/*.ts', 'src/**/*.tsx'],
            exclude: [
                'src/main.tsx',
                'src/App.tsx',             // Global app entry router shell
                'src/vite-env.d.ts',
                'src/types/**',            // Pure TypeScript type/interface contracts
                'src/components/**',       // Views & guards (Dashboard, AdminConsole, guards)
                'src/assets/**',
                'src/styles/**',
                '**/*.test.tsx',
                '**/*.test.ts',
                'src/api/**'
            ],
            thresholds: {
                lines: 95,
                branches: 95,
                functions: 95,
                statements: 95
            }
        }
    }
})
