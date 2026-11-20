import { Badge } from "@/components/ui/badge";
import type { EventStatus } from "@/services/api";

const LABELS: Record<EventStatus, { label: string; variant: "default" | "secondary" | "destructive" }> = {
  OPEN: { label: "Ouvert aux dépôts", variant: "default" },
  CLOSED: { label: "Fermé", variant: "secondary" },
  SUSPENDED: { label: "Suspendu", variant: "destructive" },
};

export function EventStatusBadge({ status }: { status: EventStatus }) {
  const { label, variant } = LABELS[status];
  return <Badge variant={variant}>{label}</Badge>;
}
