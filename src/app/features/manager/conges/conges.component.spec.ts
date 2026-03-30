import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CongesComponent } from './conges.component';

describe('CongesComponent', () => {
  let component: CongesComponent;
  let fixture: ComponentFixture<CongesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CongesComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(CongesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
