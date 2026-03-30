import { ComponentFixture, TestBed } from '@angular/core/testing';

import { IndicateursComponent } from './indicateurs.component';

describe('IndicateursComponent', () => {
  let component: IndicateursComponent;
  let fixture: ComponentFixture<IndicateursComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [IndicateursComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(IndicateursComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
