import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'tndCurrency',
  standalone: true
})
export class TndCurrencyPipe implements PipeTransform {

  transform(
    value: number | string | null | undefined,
    decimals: number = 0,
    symbol: string = 'DT'
  ): string {
    if (value === null || value === undefined || value === '') {
      return `0 ${symbol}`;
    }

    const numericValue = Number(value);

    if (Number.isNaN(numericValue)) {
      return `0 ${symbol}`;
    }

    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals
    }).format(numericValue);

    return `${formatted} ${symbol}`;
  }
}