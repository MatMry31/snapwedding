import { FormEvent, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import type { EventInput } from "@/services/api";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  submitLabel: string;
  initial?: EventInput;
  onSubmit: (input: EventInput) => Promise<unknown>;
}

/** Formulaire de création / modification : il n'envoie que name, eventDate et location (les champs du DTO). */
export function EventFormDialog({ open, onOpenChange, title, submitLabel, initial, onSubmit }: Props) {
  const [name, setName] = useState("");
  const [eventDate, setEventDate] = useState("");
  const [location, setLocation] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setName(initial?.name ?? "");
      setEventDate(initial?.eventDate ?? "");
      setLocation(initial?.location ?? "");
    }
  }, [open, initial]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit({ name: name.trim(), eventDate, location: location.trim() || undefined });
      onOpenChange(false);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>{title}</DialogTitle>
            <DialogDescription>Mariage, anniversaire, séminaire… le nom que verront vos invités.</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="event-name">Nom</Label>
            <Input id="event-name" value={name} onChange={(e) => setName(e.target.value)}
                   maxLength={100} required placeholder="Mariage de Léa et Tom" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="event-date">Date</Label>
            <Input id="event-date" type="date" value={eventDate} onChange={(e) => setEventDate(e.target.value)} required />
          </div>
          <div className="space-y-2">
            <Label htmlFor="event-location">Lieu (facultatif)</Label>
            <Input id="event-location" value={location} onChange={(e) => setLocation(e.target.value)}
                   maxLength={200} placeholder="Château de Larroque, Toulouse" />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Annuler</Button>
            <Button type="submit" disabled={submitting}>{submitLabel}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
