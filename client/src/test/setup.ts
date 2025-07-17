/// <reference types="vitest" />
import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll, vi } from 'vitest'
import { server } from './mocks/server'

// Setup MSW
beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

// Mock environment variables
Object.defineProperty(import.meta, 'env', {
  value: {
    VITE_API_BASE_URL: 'http://localhost:3000',
    MODE: 'test',
    DEV: false,
    PROD: false,
    SSR: false,
  },
  writable: true,
})

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
    length: 0,
    key: vi.fn(),
  }
})()

Object.defineProperty(window, 'localStorage', {
  value: localStorageMock,
  writable: true,
})

// Mock sessionStorage
const sessionStorageMock = (() => {
  let store: Record<string, string> = {}
  return {
    getItem: vi.fn((key: string) => store[key] || null),
    setItem: vi.fn((key: string, value: string) => {
      store[key] = value
    }),
    removeItem: vi.fn((key: string) => {
      delete store[key]
    }),
    clear: vi.fn(() => {
      store = {}
    }),
    length: 0,
    key: vi.fn(),
  }
})()

Object.defineProperty(window, 'sessionStorage', {
  value: sessionStorageMock,
  writable: true,
})

// Mock window.location
Object.defineProperty(window, 'location', {
  value: {
    href: 'http://localhost:3000',
    origin: 'http://localhost:3000',
    pathname: '/',
    search: '',
    hash: '',
    assign: vi.fn(),
    replace: vi.fn(),
    reload: vi.fn(),
  },
  writable: true,
})

// Mock ResizeObserver
global.ResizeObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}))

// Mock IntersectionObserver
global.IntersectionObserver = vi.fn().mockImplementation(() => ({
  observe: vi.fn(),
  unobserve: vi.fn(),
  disconnect: vi.fn(),
}))

// Mock matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
})

// Mock scrollTo
window.scrollTo = vi.fn() as any

// Mock File and FileReader
global.File = class MockFile {
  constructor(public fileParts: any[], public fileName: string, public options: any = {}) {
    this.name = fileName
    // Calculate size based on content
    this.size = fileParts.reduce((total, part) => {
      if (typeof part === 'string') {
        return total + part.length
      }
      return total + (part.byteLength || part.length || 0)
    }, 0)
    this.type = options.type || 'text/plain'
    this.lastModified = Date.now()
  }
  name: string
  size: number
  type: string
  lastModified: number
  webkitRelativePath: string = ''
  arrayBuffer(): Promise<ArrayBuffer> { return Promise.resolve(new ArrayBuffer(0)) }
  bytes(): Promise<Uint8Array> { return Promise.resolve(new Uint8Array(0)) }
  slice(): Blob { return new Blob() }
  stream(): ReadableStream { return new ReadableStream() }
  text(): Promise<string> { return Promise.resolve('') }
} as any

global.FileReader = class MockFileReader {
  static readonly EMPTY = 0
  static readonly LOADING = 1
  static readonly DONE = 2
  
  readAsDataURL = vi.fn()
  readAsText = vi.fn()
  result: string | ArrayBuffer | null = null
  error: any = null
  onload: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  onerror: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  onabort: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  onloadstart: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  onloadend: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  onprogress: ((this: FileReader, ev: ProgressEvent<FileReader>) => any) | null = null
  addEventListener = vi.fn()
  removeEventListener = vi.fn()
  dispatchEvent = vi.fn()
  abort = vi.fn()
  readyState = 0
  EMPTY = 0
  LOADING = 1
  DONE = 2
} as any

// Mock crypto.randomUUID
Object.defineProperty(global, 'crypto', {
  value: {
    randomUUID: () => 'test-uuid-' + Math.random().toString(36).substring(2, 11),
  },
})