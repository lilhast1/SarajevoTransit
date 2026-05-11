/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        accent: 'var(--accent)',
        surface: 'var(--surface)',
        'surface-soft': 'var(--surface-soft)',
        border: 'var(--border)',
        ink: 'var(--ink)',
        muted: 'var(--muted)',
      },
      boxShadow: {
        panel: '0 0 0 rgba(0, 0, 0, 0)',
      },
      borderRadius: {
        panel: '4px',
      },
    },
  },
  plugins: [],
}
