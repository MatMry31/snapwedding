import { ReactNode } from "react";
import { Button } from "@/components/ui/button";
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

interface Props {
  label?: string;
  icon: ReactNode;
  /** Bouton icône seule (dans une liste, sur une photo...) */
  iconOnly?: boolean;
  destructive?: boolean;
  size?: "default" | "sm" | "icon";
  title: string;
  description: string;
  onConfirm: () => void;
}

/** Bouton qui demande une confirmation avant une action irréversible. */
export function ConfirmButton({ label, icon, iconOnly, destructive, size, title, description, onConfirm }: Props) {
  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        {iconOnly ? (
          <Button variant={destructive ? "destructive" : "ghost"} size={size ?? "icon"} aria-label={title}>{icon}</Button>
        ) : (
          <Button variant={destructive ? "destructive" : "outline"} size={size}>{icon} {label}</Button>
        )}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{title}</AlertDialogTitle>
          <AlertDialogDescription>{description}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annuler</AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm}>Confirmer</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
