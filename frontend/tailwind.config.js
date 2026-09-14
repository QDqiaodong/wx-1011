/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: '#0077b6',
        secondary: '#00b4d8',
        accent: '#48cae4',
        neutral: '#f0f9ff',
      },
    },
  },
  plugins: [],
}