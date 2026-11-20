import { useState } from "react";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { format, parseISO } from "date-fns";
import { fr } from "date-fns/locale";
import { Eye, EyeOff, ImageOff, Trash2 } from "lucide-react";
import { toast } from "sonner";
import { Album, api, OrganizerMedia } from "@/services/api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ConfirmButton } from "@/components/ConfirmButton";

/**
 * O5 + O6 : toutes les photos de l'événement (masquées comprises), avec le pseudo de l'invité,
 * et les actions de modération. Chargement par pages de 48 (« Voir plus »).
 */
export function MediaGallery({ eventId, albums }: { eventId: string; albums: Album[] }) {
  const queryClient = useQueryClient();
  const [albumId, setAlbumId] = useState<string | undefined>(undefined);
  const key = ["media", eventId, albumId ?? "all"];

  const { data, isLoading, fetchNextPage, hasNextPage, isFetchingNextPage } = useInfiniteQuery({
    queryKey: key,
    queryFn: ({ pageParam }) => api.getEventMedia(eventId, pageParam, albumId),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page.number + 1 < last.page.totalPages ? last.page.number + 1 : undefined),
  });

  const photos = data?.pages.flatMap((p) => p.content) ?? [];
  const total = data?.pages[0]?.page.totalElements ?? 0;
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["media", eventId] });
  const onError = (e: Error) => toast.error(e.message);

  const toggle = useMutation({
    mutationFn: (m: OrganizerMedia) => (m.hidden ? api.showMedia(eventId, m.id) : api.hideMedia(eventId, m.id)),
    onSuccess: (m) => { refresh(); toast.success(m.hidden ? "Photo masquée aux invités" : "Photo de nouveau visible"); },
    onError,
  });
  const remove = useMutation({
    mutationFn: (id: string) => api.deleteMedia(eventId, id),
    onSuccess: () => { refresh(); toast.success("Photo supprimée"); },
    onError,
  });

  const albumName = (id: string) => albums.find((a) => a.id === id)?.name ?? "";

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Photos {total > 0 && <span className="text-muted-foreground font-normal">({total})</span>}</CardTitle>
        <CardDescription>Toutes les photos déposées par vos invités. Les photos masquées ne sont plus visibles par eux.</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {albums.length > 0 && (
          <div className="flex flex-wrap gap-2" role="group" aria-label="Filtrer par album">
            <Button size="sm" variant={albumId ? "outline" : "default"} onClick={() => setAlbumId(undefined)}>Tous</Button>
            {albums.map((a) => (
              <Button key={a.id} size="sm" variant={albumId === a.id ? "default" : "outline"} onClick={() => setAlbumId(a.id)}>
                {a.name}
              </Button>
            ))}
          </div>
        )}

        {isLoading && (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="aspect-square" />)}
          </div>
        )}

        {!isLoading && photos.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-10 text-center text-muted-foreground">
            <ImageOff className="h-8 w-8" />
            <p>Aucune photo pour l'instant.</p>
            <p className="text-sm">Vos invités pourront en déposer en scannant le QR code.</p>
          </div>
        )}

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
          {photos.map((photo) => (
            <figure key={photo.id} className="overflow-hidden rounded-md border bg-card">
              <div className="relative">
                <img src={photo.url} alt={`Photo de ${photo.guestNickname}`} loading="lazy"
                     className={`aspect-square w-full object-cover ${photo.hidden ? "opacity-40" : ""}`} />
                {photo.hidden && <Badge variant="secondary" className="absolute left-2 top-2">Masquée</Badge>}
              </div>
              <figcaption className="flex items-center justify-between gap-1 p-2">
                <div className="min-w-0 text-xs">
                  <p className="truncate font-medium">{photo.guestNickname}</p>
                  <p className="truncate text-muted-foreground">
                    {albumName(photo.albumId)} · {format(parseISO(photo.createdAt), "d MMM HH:mm", { locale: fr })}
                  </p>
                </div>
                <div className="flex shrink-0">
                  <Button variant="ghost" size="icon" onClick={() => toggle.mutate(photo)}
                          aria-label={photo.hidden ? "Réafficher la photo" : "Masquer la photo"}>
                    {photo.hidden ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                  </Button>
                  <ConfirmButton
                    icon={<Trash2 className="h-4 w-4" />} iconOnly
                    title="Supprimer cette photo ?"
                    description={`La photo de ${photo.guestNickname} sera définitivement supprimée.`}
                    onConfirm={() => remove.mutate(photo.id)}
                  />
                </div>
              </figcaption>
            </figure>
          ))}
        </div>

        {hasNextPage && (
          <Button variant="outline" className="w-full" onClick={() => fetchNextPage()} disabled={isFetchingNextPage}>
            Voir plus de photos
          </Button>
        )}
      </CardContent>
    </Card>
  );
}
