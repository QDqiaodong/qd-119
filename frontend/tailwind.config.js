export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{js,ts,vue}"],
  theme: {
    container: {
      center: true,
    },
    extend: {
      colors: {
        primary: {
          50: '#E8EEF4',
          100: '#C5D4E3',
          200: '#9FB7CF',
          300: '#799ABB',
          400: '#5C83AC',
          500: '#3F6B9D',
          600: '#355F90',
          700: '#294F7D',
          800: '#1B4F72',
          900: '#0F3A56',
        },
        accent: {
          50: '#FDF2E6',
          100: '#FAE0BD',
          200: '#F7CB90',
          300: '#F4B562',
          400: '#F0A03A',
          500: '#E67E22',
          600: '#CC6B14',
          700: '#A85710',
          800: '#84430C',
          900: '#602F08',
        },
        danger: '#E74C3C',
        success: '#27AE60',
      },
      fontFamily: {
        sans: ['"Noto Sans SC"', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
