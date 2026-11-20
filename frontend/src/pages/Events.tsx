import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { fr } from "date-fns/locale";
import { CalendarDays, MapPin, Plus } from "lucide-react";
import { toast } from "sonner";
import { api, EventInput } from "@/services/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { EventStatusBadge } from "@/components/EventStatusBadge";
import { EventFormDialog } from "@/components/EventFormDialog";

/** O2 : la liste des événements de l'organisateur connecté (le backend filtre par tenant). */
export default function Events() {
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({ queryKey: ["events"], queryFn: () => api.getEvents() });

  const create = useMutation({
    mutationFn: (input: EventInput) => api.createEvent(input),
    onSuccess: (event) => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
      toast.success("Événement créé");
      navigate(`/events/${event.id}`);
    },
    onError: (e: Error) => toast.error(e.message),
  });

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Mes événements</h1>
          <p className="text-muted-foreground text-sm">Créez un événement, puis imprimez son QR code pour les tables.</p>
        </div>
        <Button onClick={() => setCreating(true)}>
          <Plus className="h-4 w-4" /> Nouvel événement
        </Button>
      </div>

      {isLoading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[0, 1, 2].map((i) => <Skeleton key={i} className="h-32" />)}
        </div>
      )}

      {error && <p className="text-destructive">{(error as Error).message}</p>}

      {data && data.content.length === 0 && (
        <Card>
          <CardContent className="py-10 text-center space-y-3">
            <p className="font-medium">Aucun événement pour l'instant</p>
            <p className="text-sm text-muted-foreground">Commencez par créer votre premier événement.</p>
            <Button onClick={() => setCreating(true)}><Plus className="h-4 w-4" /> Créer un événement</Button>
          </CardContent>
        </Card>
      )}

      {data && data.content.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {data.content.map((event) => (
            <Link key={event.id} to={`/events/${event.id}`}>
              <Card className="h-full transition-colors hover:border-primary">
                <CardHeader className="space-y-2">
                  <EventStatusBadge status={event.status} />
                  <CardTitle className="text-lg">{event.name}</CardTitle>
                </CardHeader>
                <CardContent className="space-y-1 text-sm text-muted-foreground">
                  <p className="flex items-center gap-2">
                    <CalendarDays className="h-4 w-4" />
                    {format(parseISO(event.eventDate), "d MMMM yyyy", { locale: fr })}
                  </p>
                  {event.location && (
                    <p className="flex items-center gap-2"><MapPin className="h-4 w-4" />{event.location}</p>
                  )}
                </CardContent>
              </Card>
            </Link>
          ))}
        </div>
      )}

      <EventFormDialog
        open={creating}
        onOpenChange={setCreating}
        title="Nouvel événement"
        submitLabel="Créer"
        onSubmit={(input) => create.mutateAsync(input)}
      />
    </div>
  );
}
