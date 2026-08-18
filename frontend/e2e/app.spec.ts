import {test, expect} from '@playwright/test';

test.describe('Trace Initiator', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:4200');
  });

  test('should reverse a string using the real backend', async ({page}) => {
    await page.locator('#string-argument').fill('ABC123');
    await page.getByRole('button', {name: 'Reverse'}).click();
    await expect(page.locator('.result')).toContainText('321CBA');
  });

  test('should reverse an empty string using the real backend', async ({page}) => { 
    await page.getByRole('button', {name: 'Reverse'}).click(); 
    await expect(page.locator('.result')).toContainText('Result:'); 
  });

  test('should calculate length using the real backend', async ({page}) => {
    await page.locator('#string-argument').fill('ABC123');
    await page.getByRole('button', {name: 'Length'}).click();
    await expect(page.locator('.result')).toContainText('6');
  });

  test('should calculate length of an empty string using the real backend', async ({page}) => {
    await page.getByRole('button', {name: 'Length'}).click(); 
    await expect(page.locator('.result')).toContainText('Result: 0'); 
});

  test('should display the backend exception', async ({page}) => {
    await page.getByRole('button', {name: 'Exception'}).click();
    await expect(page.locator('#string-argument')).toHaveValue('ERROR');
    await expect(page.locator('.result')).toContainText("Reverse failed (HTTP 400, Input cannot be 'ERROR')",);
  });

  test('should mix reverse and length responses', async ({page}) => {
    await page.locator('#string-argument').fill('ABC');
    await page.getByRole('button', {name: 'Mix'}).click();
    await expect(page.locator('.result')).toContainText('Reverted string: CBA');
    await expect(page.locator('.result')).toContainText('String length: 3');
  });
});
