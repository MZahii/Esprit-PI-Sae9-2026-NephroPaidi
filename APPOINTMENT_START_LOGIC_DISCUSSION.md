# Appointment Start Logic - Discussion Document

## Current Implementation (Draft - Under Review)

### What We Built
```
Appointment scheduled for: 10:00 AM (scheduledAt)

START BUTTON BEHAVIOR:
- Button appears: ONLY if appointment date = TODAY
- Button enabled: If NOW is within [10:00 AM, 10:15 AM] (15-min window)
- Button disabled: If NOW < 10:00 AM (appointment hasn't started yet)
- Button disabled: If NOW > 10:15 AM (window closed - appointment auto-cancelled)

AUTO-CANCEL JOB (runs every 60 seconds):
- If appointment.scheduledAt + 15 minutes < NOW
- AND appointment.status = SCHEDULED
- THEN: status → CANCELLED, notification sent to patient
```

### Issues With Current Logic

1. **No buffer time for doctor arrival**
   - Doctor might be in another room/consultation
   - Takes 2-3 minutes to get to consultation room
   - Should doctor be able to start 5-10 minutes BEFORE appointment?

2. **Rigid 15-minute window is harsh**
   - What if doctor is genuinely 5 minutes late due to clinic reasons?
   - Auto-cancel might be too aggressive
   - Patient might not cancel - they might still be waiting

3. **No distinction between:**
   - Doctor not showing up (should cancel)
   - Doctor running late but arriving (should wait)
   - System not aware doctor is delayed

4. **Patient experience unclear**
   - Patient sees appointment cancelled but doesn't know why
   - No way for patient to request extension
   - No way for doctor to notify they're running late

5. **What about rescheduling?**
   - Once cancelled, can doctor offer immediate reschedule?
   - Or is it lost?

---

## Key Questions We Need to Answer

### **Q1: When should the START button first appear?**

**Option A (Current)**: Button appears at EXACTLY appointment.scheduledAt
```
10:00 AM appointment:
- 9:59 AM: Button DISABLED ("Appointment not ready yet")
- 10:00 AM: Button ENABLED (can start now)
- 10:15 AM: Button DISABLED + auto-cancel
```

**Option B**: Button appears 5-10 minutes BEFORE appointment
```
10:00 AM appointment:
- 9:50 AM: Button ENABLED ("Appointment ready, doctor can prepare")
- 10:00-10:15 AM: Button ENABLED
- 10:15+ AM: Button DISABLED + auto-cancel
```

**Option C**: Button appears 3-5 minutes BEFORE appointment
```
Similar to B but smaller buffer
```

**Analysis**:
- Clinic doctors often move between rooms
- 5-10 min prep is realistic
- But... might allow doctor to start too early?

---

### **Q2: What is the exact 15-minute window logic?**

**Current understanding**:
```
Window = [scheduledAt, scheduledAt + 15 minutes)
```

**But what does "15 minutes" mean?**

**Interpretation A**: Doctor has 15 minutes FROM appointment time to start
```
10:00 AM appointment:
- Can start: 10:00 - 10:15 AM
- After 10:15 AM: Appointment is "missed" → AUTO-CANCEL
```

**Interpretation B**: Doctor has 15 minutes OF GRACE for lateness
```
10:00 AM appointment:
- Expected start: 10:00 AM
- Grace period (tolerated lateness): 15 minutes
- Auto-cancel at: 10:15 AM (if not started)
```

**Interpretation C**: Consultation must START within 15 min window but can CONTINUE after
```
10:00 AM appointment:
- Can start: 10:00 - 10:15 AM
- Once started: Consultation can run as long as needed
- Auto-cancel only if NOT started by 10:15 AM
```

**Most Clinic-Logical**: Interpretation C
- Doctor has 15-minute window to BEGIN consultation
- Once started, consultation duration is separate (doctor controls)
- Ensures appointment doesn't get lost

---

### **Q3: Who should initiate cancellation?**

**Option A (Current)**: Automatic system cancellation
```
After 15 minutes of no action:
- System automatically cancels appointment
- Patient notified: "Appointment was cancelled (no-show)"
- Doctor never sees option to start
```
**Pros**: Automated, no manual intervention  
**Cons**: Harsh, no human override, patient might be in waiting room

**Option B**: Patient initiates cancellation
```
After 15 minutes:
- Patient gets notification: "Doctor hasn't arrived. Cancel or continue waiting?"
- Patient can: (1) Cancel, (2) Keep waiting, (3) Request reschedule
- Auto-cancel only after 30-45 min total
```
**Pros**: Patient has control, more humane  
**Cons**: Requires patient action

**Option C**: Two-stage cancellation
```
At 10:15 AM (end of 15-min window):
- Appointment status: "MISSED" (not fully cancelled yet)
- Doctor still sees it but marked as MISSED
- Receptionist can manually confirm cancellation
- Auto-hard-cancel after 30 minutes of MISSED state
```
**Pros**: Flexible, allows human review  
**Cons**: More complex

---

### **Q4: What about appointment duration?**

**Current assumption**: Not explicitly addressed

**Questions**:
- Does appointment have a `durationMinutes` field? YES ✅ (from existing Appointment entity)
- Should doctor be able to start consultation if < durationMinutes remaining in day?
  - E.g., 2-hour appointment starting at 4:55 PM (day ends 5:00 PM)
- Should system warn doctor?

**Example**:
```
Appointment: 4:55 PM - duration 2 hours
Doctor tries to start consultation at 4:55 PM
System response options:
  A) Allow start, consultation runs past clinic hours
  B) Warn: "Only 5 min left in clinic day. Continue anyway?"
  C) Block: "Insufficient time remaining. Reschedule."
```

---

### **Q5: What about a doctor being legitimately delayed?**

**Scenario**: 
```
10:00 AM appointment with Patient A
Doctor in 9:30 AM appointment with Patient B (running 10 min late)
9:50 AM: Doctor finishes with Patient B, starts walking to Patient A's room
10:05 AM: Doctor arrives at Patient A's room, tries to start consultation
10:15 AM: System auto-cancels because doctor arrived after window closed
```

**Solution approaches**:

**A) Give doctor a "Delayed Notification" option**
```
At 9:55 AM: Doctor can send notification to Patient A:
"Running 5 minutes late, still coming"
- Resets the 15-min window clock?
- Or just informs patient but doesn't extend window?
```

**B) Allow receptionist to extend window**
```
If receptionist sees doctor is delayed:
- Receptionist can grant 5-10 min extension
- Sends message to patient: "Doctor is running late, please wait"
```

**C) Receptionist knows doctor schedule**
```
System knows doctor is finishing previous appointment
- Automatically extends window based on doctor's calendar
- More intelligent, less manual
```

---

### **Q6: What happens after appointment starts?**

**Current assumption**: Doctor clicks "Start Consultation" → appointment.status = CONFIRMED → consultation happens

**Questions**:
- Can doctor pause/stop and restart?
- Is there an "End Consultation" button?
- Where does notes/diagnosis get recorded?
- Is consultation duration tracked automatically?

**Example workflow**:
```
10:05 AM: Doctor starts consultation (status = CONFIRMED)
10:07 AM: Doctor realizes needs lab test, puts patient on hold
10:12 AM: Doctor sends lab request, patient goes to lab
10:30 AM: Patient returns, doctor resumes
10:35 AM: Doctor ends consultation (status = COMPLETED)
```

**Question**: Is this supported?

---

## Scenarios to Test (Before Implementation)

### Scenario 1: Doctor on time
```
GIVEN: Appointment 10:00 AM
WHEN: Doctor clicks START at 10:05 AM
THEN: 
  ✓ Consultation starts
  ✓ Status changes to CONFIRMED
  ✓ Patient notified
  ✓ No auto-cancel trigger
```
✅ **Current logic handles this**

---

### Scenario 2: Doctor slightly late (within window)
```
GIVEN: Appointment 10:00 AM
WHEN: Doctor clicks START at 10:12 AM (after 15-min window passes)
THEN:
  ✗ Current logic: Appointment auto-cancelled, doctor cannot start
  ✓ Desired behavior: ???
```
❓ **Need to decide**: Should this be allowed?

---

### Scenario 3: Doctor very late (no-show)
```
GIVEN: Appointment 10:00 AM
WHEN: Now 10:25 AM and doctor hasn't shown up
THEN:
  ✓ Appointment should be cancelled
  ✓ Patient notified
  ✓ System prevents doctor from retroactively starting
```
✅ **Current logic handles this** (but maybe too aggressively at 10:15)

---

### Scenario 4: Patient arrives late
```
GIVEN: Appointment 10:00 AM (doctor ready at 10:05 AM)
WHEN: Patient arrives at 10:18 AM
THEN:
  ✗ Appointment already auto-cancelled by system
  ✓ Desired behavior: ???
```
❓ **Need to decide**: Should receptionist be able to "reactivate" cancelled appointments?

---

### Scenario 5: Doctor wants to start early (prep time)
```
GIVEN: Appointment 10:00 AM
WHEN: Doctor clicks START at 9:50 AM
THEN:
  ✗ Current logic: Button disabled, cannot start
  ✓ Desired behavior: ???
```
❓ **Need to decide**: Is prep time before appointment time allowed?

---

### Scenario 6: Multiple appointments back-to-back
```
GIVEN: 
  - 10:00 AM appointment with Patient A (1 hour)
  - 10:15 AM appointment with Patient B (30 min)
  - 10:45 AM appointment with Patient C (30 min)

WHEN: Doctor starts consultation with A at 10:05 AM (15 min late)
THEN:
  ✗ What happens to appointments B and C?
  ✓ Are they automatically rescheduled?
  ✓ Do they get cancelled after their window passes?
```
❓ **Complex scenario** - need workflow definition

---

## Proposed Workflow (For Discussion)

### **OPTION 1: Clinic-Friendly (Flexible)**

**Before appointment (pre-arrival phase)**:
- 5-10 min before appointment: Start button appears and is ENABLED
- Doctor can preview patient info, prepare
- Doctor not yet in consultation room with patient

**Appointment window (30 minutes)**:
- Appointment time: Doctor should be ready
- Up to appointment time + 30 minutes: Doctor can still start consultation
  - First 15 min: "On time"
  - 15-30 min: "Late but acceptable"
  - After 30 min: "Missed" → Auto-cancel, receptionist can manually reactivate

**Delayed notification**:
- Doctor can send "Running 5 min late" message to patient at any time
- Extends tolerance by 10 minutes from send time
- Patient sees notification, can wait or cancel

**Receptionist override**:
- Receptionist can manually extend window before auto-cancel
- Receptionist can reactivate cancelled appointments
- Used when doctor is in emergency or patient is late

---

### **OPTION 2: Strict (No-Show Prevention)**

**Appointment window (exactly 15 minutes)**:
- Appointment time: Window starts
- Appointment time + 15 min: Window closes, auto-cancel
- No exceptions, no extensions
- Enforces clinic punctuality strictly

**Trade-off**:
- ✅ Prevents no-shows
- ❌ Harsh on legitimate delays
- ❌ High cancellation rate

---

### **OPTION 3: Hybrid (Balanced)**

**Appointment window (20 minutes)**:
- Appointment time: Window starts
- Appointment time + 20 min: Auto-cancel
- Receptionist can extend +10 min if needed (emergency, doctor notified system)
- Patient gets notification at 15 min: "Doctor running 5 min late"

**Notification system**:
- Doctor can flag "Running late" at appointment time or earlier
- Patient sees notification, can wait or request reschedule
- System tracks lateness patterns per doctor (analytics)

---

## Decision Matrix (What We Need to Decide)

| Decision | Option A | Option B | Option C | Current Draft |
|----------|----------|----------|----------|---------------|
| **Buffer time before appt** | 0 min | 5-10 min | 3-5 min | 0 min |
| **Window duration** | 15 min | 20-30 min | 20 min | 15 min |
| **Auto-cancel trigger** | Strict | Flexible | Moderate | Strict |
| **Receptionist override** | No | Yes | Yes | No |
| **Doctor "running late" notify** | No | Yes | Yes | No |
| **Patient can extend** | No | Yes | No | No |
| **Complexity** | Low | High | Medium | Low |
| **Clinic-friendly** | ❌ | ✅ | ✅✅ | ❌ |
| **No-show prevention** | ✅✅ | ❌ | ✅ | ✅✅ |

---

## Questions for Clinic Staff (If Available)

1. **How often do doctors run late?** (Few min vs 15+ min?)
2. **How long does appointment prep take?** (5 min? 10 min?)
3. **Should appointment be lost if doctor is 10 min late?**
4. **Can receptionist intervene?** (extend window, reactivate cancelled)
5. **Do patients often arrive late?**
6. **What's reasonable grace period for "missed" appointment?**
7. **How should back-to-back appointments handle delays?**

---

## Recommendation (Before User Input)

**PROPOSE: OPTION 3 (Hybrid/Balanced)**

**Reasons**:
- ✅ Realistic for clinic operations (doctors do run late sometimes)
- ✅ Still enforces punctuality (20-min window is reasonable)
- ✅ Gives receptionist control (can extend +10 min in emergencies)
- ✅ Patient-friendly (notified, not surprised by cancellation)
- ✅ Doctor-friendly (legitimate lateness accommodated)
- ✅ Prevents abuse (still auto-cancels after reasonable time)

**Logic**:
```
APPOINTMENT WINDOW:

Timeline:
  T-5 min:  Start button ENABLED ("Prepare consultation")
  T        : Appointment scheduled time
  T+20 min : Window CLOSES, status → "MISSED"
  T+30 min : HARD CANCEL if receptionist hasn't reactivated

Receptionist can:
  - At any point before T+20 min: Extend window +10 min
  - At any point before T+30 min: Reactivate cancelled appointment
  - Add note: "Patient running late" or "Doctor in emergency"

Doctor can:
  - At T-5 min onwards: Send "Running late" notification
  - This doesn't extend window but notifies patient
  - Still must start consultation by T+20 min

Patient experience:
  - At T: Notification "Consultation starting"
  - At T+15 min (if not started): Notification "Doctor running late, patience appreciated"
  - At T+20 min (if still not started): Notification "Appointment cancelled. Contact clinic to reschedule"
```

---

## Next Steps (After Discussion)

1. ✅ **User reviews** this document
2. ✅ **User selects** preferred workflow option (or proposes alternative)
3. ✅ **User clarifies** edge cases and scenarios
4. ✅ **User confirms** requirements for:
   - Prep time window
   - Grace period duration
   - Receptionist capabilities
   - Patient notifications
   - Cancellation logic
5. ✅ **THEN we implement** with full clarity

---

## Implementation Considerations (Once Decision Made)

**Backend changes needed**:
- AppointmentStartService: Logic for window validation
- AppointmentRepository: Queries for stale/missed appointments
- Scheduled job: Auto-cancel at window close + hard-cancel at limit
- NotificationService: Send messages to patient/doctor
- ReceptionistService: Extend window, reactivate appointment

**Frontend changes needed**:
- Button state logic: enabled/disabled based on window
- Countdown timer: Show time remaining
- Notification display: Toast alerts for status changes
- Receptionist UI: Override buttons for extension/reactivation
- Doctor UI: "Running late" quick button

**Database changes needed**:
- Appointment: Add fields like `window_extended_by`, `override_reason`, `notification_sent_at`
- Or extend logic without schema changes (use existing fields)

