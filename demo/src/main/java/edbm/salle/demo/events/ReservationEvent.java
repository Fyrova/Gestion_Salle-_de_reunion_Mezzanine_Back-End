package edbm.salle.demo.events;

import edbm.salle.demo.model.Reservation;

public class ReservationEvent {
    private final Reservation reservation;
    private final String action;
    private final boolean equipmentAffected;
    private final boolean isRecurring;
    private final String recurrenceDetails;

    public ReservationEvent(Reservation reservation, String action, boolean equipmentAffected, boolean isRecurring, String recurrenceDetails) {
        this.reservation = reservation;
        this.action = action;
        this.equipmentAffected = equipmentAffected;
        this.isRecurring = isRecurring;
        this.recurrenceDetails = recurrenceDetails;
    }

    public Reservation getReservation() { return reservation; }
    public String getAction() { return action; }
    public boolean isEquipmentAffected() { return equipmentAffected; }
    public boolean isRecurring() { return isRecurring; }
    public String getRecurrenceDetails() { return recurrenceDetails; }
}
