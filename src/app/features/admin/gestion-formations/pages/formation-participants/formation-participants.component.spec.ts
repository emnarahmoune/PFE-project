import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormationParticipantsComponent } from './formation-participants.component';

describe('FormationParticipantsComponent', () => {
  let component: FormationParticipantsComponent;
  let fixture: ComponentFixture<FormationParticipantsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormationParticipantsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(FormationParticipantsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
