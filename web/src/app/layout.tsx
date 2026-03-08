import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { PHProvider } from "./providers";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "UberDriverCompanion — Metricas en tiempo real para conductores",
  description:
    "App Android que analiza viajes de Uber Driver en tiempo real. Muestra $/km, $/hora y % de distancia cobrada en un overlay flotante para tomar mejores decisiones.",
  openGraph: {
    title: "UberDriverCompanion",
    description:
      "Metricas en tiempo real sobre cada viaje de Uber, directo en tu pantalla.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >
        <PHProvider>{children}</PHProvider>
      </body>
    </html>
  );
}
