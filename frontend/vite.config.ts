import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
    plugins: [
        tailwindcss(),
        react()
    ],
    test: {
        globals: true,
        environment: 'jsdom',
        setupFiles: ['./src/test/setup.ts'],
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
