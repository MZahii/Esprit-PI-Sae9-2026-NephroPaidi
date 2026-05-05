import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { HrList } from './hr-list';

describe('HrList', () => {
  let component: HrList;
  let fixture: ComponentFixture<HrList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HrList],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(HrList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
