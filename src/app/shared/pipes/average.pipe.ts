import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'average',
  standalone: true
})
export class AveragePipe implements PipeTransform {
  transform(values: number[]): number | null {
    if (!values || values.length === 0) return null;
    const validValues = values.filter(v => !isNaN(v) && v !== null);
    if (validValues.length === 0) return null;
    const sum = validValues.reduce((acc, val) => acc + val, 0);
    return sum / validValues.length;
  }
}