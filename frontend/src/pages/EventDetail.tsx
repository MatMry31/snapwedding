import { FormEvent, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { fr } from "date-fns/locale";
import { ArrowLeft, CalendarDays, Download, Lock, MapPin, Pencil, Plus, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Album, api, EventInput } from "@/services/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { EventStatusBadge } from "@/components/EventStatusBadge";
import { EventFormDialog } from "@/components/EventFormDialog";

export default function EventDetail() {
  const { eventId = "" } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState(false);

  const { data: event, isLoading, error } = useQuery({
    queryKey: ["event", eventId],
    queryFn: () => api.getEvent(eventId),
  });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["event", eventId] });
    queryClient.invalidateQueries({ queryKey: ["events"] });
  };

  const update = useMutation({
    mutationFn: (input: EventInput) => api.updateEvent(eventId, input),
    onSuccess: () => { refresh(); toast.success("Événement modifié"); },
    onError: (e: Error) => toast.error(e.message),
  });

  const close = useMutation({
    mutationFn: () => api.closeEvent(eventId),
    onSuccess: () => { refresh(); toast.success("Événement fermé : plus aucun dépôt possible"); },
    onError: (e: Error) => toast.error(e.message),
  });

  const remove = useMutation({
    mutationFn: () => api.deleteEvent(eventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["events"] });
      toast.success("Événement supprimé");
      navigate("/events");
    },
    onError: (e: Error) => toast.error(e.message),
  });

  if (isLoading) return <Skeleton className="h-64" />;
  if (error || !event) {
    return (
      <div className="space-y-4">
        <p className="text-destructive">{(error as Error)?.message ?? "Événement introuvable"}</p>
        <Button variant="outline" asChild><Link to="/events">Retour</Link></Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" asChild>
        <Link to="/events"><ArrowLeft className="h-4 w-4" /> Mes événements</Link>
      </Button>

      {/* En-tête : infos + actions */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <EventStatusBadge status={event.status} />
          <h1 className="text-2xl font-bold">{event.name}</h1>
          <div className="flex flex-wrap gap-4 text-sm text-muted-foreground">
            <span className="flex items-center gap-2">
              <CalendarDays className="h-4 w-4" />
              {format(parseISO(event.eventDate), "EEEE d MMMM yyyy", { locale: fr })}
            </span>
            {event.location && <span className="flex items-center gap-2"><MapPin className="h-4 w-4" />{event.location}</span>}
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setEditing(true)} disabled={event.status === "SUSPENDED"}>
            <Pencil className="h-4 w-4" /> Modifier
          </Button>

          {event.status === "OPEN" && (
            <ConfirmButton
              label="Fermer" icon={<Lock className="h-4 w-4" />}
              title="Fermer l'événement ?"
              description="Les invités ne pourront plus déposer de photos. L'album restera consultable."
              onConfirm={() => close.mutate()}
            />
          )}

          <ConfirmButton
            label="Supprimer" icon={<Trash2 className="h-4 w-4" />} destructive
            title="Supprimer l'événement ?"
            description="L'événement et tous ses albums seront définitivement supprimés."
            onConfirm={() => remove.mutate()}
          />
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[320px_1fr]">
        <QrCodeCard eventId={event.id} eventName={event.name} publicCode={event.publicCode} />
        <AlbumsCard eventId={event.id} />
      </div>

      <EventFormDialog
        open={editing}
        onOpenChange={setEditing}
        title="Modifier l'événement"
        submitLabel="Enregistrer"
        initial={{ name: event.name, eventDate: event.eventDate, location: event.location ?? undefined }}
        onSubmit={(input) => update.mutateAsync(input)}
      />
    </div>
  );
}

/* ---------- QR code (O4) ---------- */

function QrCodeCard({ eventId, eventName, publicCode }: { eventId: string; eventName: string; publicCode: string }) {
  const [qrUrl, setQrUrl] = useState<string | null>(null);
  const guestUrl = `${window.location.origin}/e/${publicCode}`;

  useEffect(() => {
    let url: string | null = null;
    let cancelled = false;
    api.getQrCodeUrl(eventId)
      .then((u) => {
        // Page quittée pendant le téléchargement : on libère tout de suite l'image
        if (cancelled) URL.revokeObjectURL(u);
        else { url = u; setQrUrl(u); }
      })
      .catch((e: Error) => { if (!cancelled) toast.error(e.message); });
    return () => {
      cancelled = true;
      if (url) URL.revokeObjectURL(url); // libère l'image en mémoire
    };
  }, [eventId]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">QR code</CardTitle>
        <CardDescription>À imprimer et poser sur les tables : vos invités le scannent pour déposer leurs photos.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {qrUrl ? (
          <img src={qrUrl} alt={`QR code de ${eventName}`} className="w-full max-w-[280px] mx-auto rounded-md border" />
        ) : (
          <Skeleton className="aspect-square w-full max-w-[280px] mx-auto" />
        )}
        <p className="text-xs text-muted-foreground break-all">{guestUrl}</p>
        <Button className="w-full" disabled={!qrUrl} asChild={!!qrUrl}>
          {qrUrl ? (
            <a href={qrUrl} download={`qrcode-${eventName}.png`}><Download className="h-4 w-4" /> Télécharger le PNG</a>
          ) : (
            <span><Download className="h-4 w-4" /> Télécharger le PNG</span>
          )}
        </Button>
      </CardContent>
    </Card>
  );
}

/* ---------- Albums (O3) ---------- */

function AlbumsCard({ eventId }: { eventId: string }) {
  const queryClient = useQueryClient();
  const [newName, setNewName] = useState("");
  const key = ["albums", eventId];

  const { data: albums, isLoading } = useQuery({ queryKey: key, queryFn: () => api.getAlbums(eventId) });
  const refresh = () => queryClient.invalidateQueries({ queryKey: key });
  const onError = (e: Error) => toast.error(e.message);

  const create = useMutation({
    mutationFn: (name: string) => api.createAlbum(eventId, name),
    onSuccess: () => { setNewName(""); refresh(); },
    onError,
  });
  const rename = useMutation({
    mutationFn: ({ id, name }: { id: string; name: string }) => api.renameAlbum(eventId, id, name),
    onSuccess: refresh,
    onError,
  });
  const remove = useMutation({
    mutationFn: (id: string) => api.deleteAlbum(eventId, id),
    onSuccess: () => { refresh(); toast.success("Album supprimé"); },
    onError,
  });

  const handleCreate = (e: FormEvent) => {
    e.preventDefault();
    if (newName.trim()) create.mutate(newName.trim());
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Albums</CardTitle>
        <CardDescription>Les invités choisiront l'album dans lequel déposer leurs photos.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <form onSubmit={handleCreate} className="flex gap-2">
          <Input value={newName} onChange={(e) => setNewName(e.target.value)} maxLength={60}
                 placeholder="Cérémonie, Vin d'honneur, Soirée…" aria-label="Nom du nouvel album" />
          <Button type="submit" disabled={!newName.trim() || create.isPending}>
            <Plus className="h-4 w-4" /> Ajouter
          </Button>
        </form>

        {isLoading && <Skeleton className="h-20" />}
        {albums && albums.length === 0 && (
          <p className="text-sm text-muted-foreground text-center py-6">Aucun album. Créez-en au moins un pour vos invités.</p>
        )}
        <ul className="divide-y rounded-md border">
          {albums?.map((album) => (
            <AlbumRow key={album.id} album={album}
                      onRename={(name) => rename.mutate({ id: album.id, name })}
                      onDelete={() => remove.mutate(album.id)} />
          ))}
        </ul>
      </CardContent>
    </Card>
  );
}

function AlbumRow({ album, onRename, onDelete }: { album: Album; onRename: (name: string) => void; onDelete: () => void }) {
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(album.name);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (name.trim() && name.trim() !== album.name) onRename(name.trim());
    setEditing(false);
  };

  return (
    <li className="flex items-center justify-between gap-2 px-3 py-2">
      {editing ? (
        <form onSubmit={submit} className="flex flex-1 gap-2">
          <Input autoFocus value={name} onChange={(e) => setName(e.target.value)} maxLength={60} aria-label="Nouveau nom" />
          <Button type="submit" size="sm">OK</Button>
        </form>
      ) : (
        <>
          <span className="font-medium">{album.name}</span>
          <div className="flex gap-1">
            <Button variant="ghost" size="icon" onClick={() => setEditing(true)} aria-label={`Renommer ${album.name}`}>
              <Pencil className="h-4 w-4" />
            </Button>
            <ConfirmButton
              icon={<Trash2 className="h-4 w-4" />} iconOnly destructive
              title={`Supprimer l'album « ${album.name} » ?`}
              description="Cette action est définitive."
              onConfirm={onDelete}
            />
          </div>
        </>
      )}
    </li>
  );
}

/* ---------- Bouton avec confirmation ---------- */

function ConfirmButton(props: {
  label?: string;
  icon: React.ReactNode;
  iconOnly?: boolean;
  destructive?: boolean;
  title: string;
  description: string;
  onConfirm: () => void;
}) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        {props.iconOnly ? (
          <Button variant="ghost" size="icon" aria-label={props.title}>{props.icon}</Button>
        ) : (
          <Button variant={props.destructive ? "destructive" : "outline"}>{props.icon} {props.label}</Button>
        )}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{props.title}</AlertDialogTitle>
          <AlertDialogDescription>{props.description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annuler</AlertDialogCancel>
          <AlertDialogAction onClick={props.onConfirm}>Confirmer</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
