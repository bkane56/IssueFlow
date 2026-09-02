import { cleanup } from '@testing-library/react'
import { afterEach, expect } from 'vitest'
import '@testing-library/jest-dom/vitest'
import * as vitestAxeMatchers from 'vitest-axe/matchers.js'

// Vitest matcher registration for accessibility smoke tests.
expect.extend(vitestAxeMatchers)

afterEach(() => {
  cleanup()
})
