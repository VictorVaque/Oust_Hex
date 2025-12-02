package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.PlayerType;
import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heurística simple y directa para Oust
 * Se enfoca en: capturas posibles, amenazas, y balance de piezas
 */
public class Heuristica {
    
    // PESOS (ajustables)
    private static final int PESO_PIEZA = 10;              // Valor por cada pieza propia
    private static final int PESO_CAPTURA = 100;           // Bonus por poder capturar
    private static final int PESO_AMENAZA = -150;          // Penalización por estar amenazado
    private static final int PESO_GRUPO_GRANDE = 20;       // Bonus por grupos grandes
    
    // Direcciones hexagonales
    private static final int[][] DIRECTIONS = {
        {0, 1}, {1, 0}, {1, 1}, {0, -1}, {-1, 0}, {-1, -1}
    };
    
    /**
     * Evalúa un estado del juego
     */
    public static int eval(GameStatus state, PlayerType maximizingPlayer) {
        PlayerType opponent = (maximizingPlayer == PlayerType.PLAYER1) 
            ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
        
        int score = 0;
        
        // 1. Balance básico de piezas
        int misPiezas = contarPiezas(state, maximizingPlayer);
        int piezasRival = contarPiezas(state, opponent);
        score += (misPiezas - piezasRival) * PESO_PIEZA;
        
        // 2. Analizar grupos y amenazas
        List<Grupo> misGrupos = obtenerGrupos(state, maximizingPlayer);
        List<Grupo> gruposRival = obtenerGrupos(state, opponent);
        
        // Valor por tamaño de grupos (grupos grandes son mejores)
        for (Grupo g : misGrupos) {
            score += g.tamano * PESO_GRUPO_GRANDE;
        }
        for (Grupo g : gruposRival) {
            score -= g.tamano * PESO_GRUPO_GRANDE;
        }
        
        // 3. Detectar amenazas y oportunidades de captura
        score += analizarAmenazas(misGrupos, gruposRival);
        
        return score;
    }
    
    /**
     * Analiza amenazas: grupos en contacto donde uno puede capturar al otro
     */
    private static int analizarAmenazas(List<Grupo> misGrupos, List<Grupo> gruposRival) {
        int score = 0;
        
        for (Grupo mio : misGrupos) {
            for (Grupo rival : gruposRival) {
                if (estanEnContacto(mio, rival)) {
                    int diferencia = mio.tamano - rival.tamano;
                    
                    if (diferencia > 0) {
                        // Puedo capturar este grupo rival
                        score += rival.tamano * PESO_CAPTURA;
                    } else if (diferencia < 0) {
                        // El rival puede capturarme
                        score += mio.tamano * PESO_AMENAZA; // PESO_AMENAZA es negativo
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Verifica si dos grupos están en contacto (son adyacentes)
     */
    private static boolean estanEnContacto(Grupo g1, Grupo g2) {
        for (Point p1 : g1.puntos) {
            for (int[] dir : DIRECTIONS) {
                Point vecino = new Point(p1.x + dir[0], p1.y + dir[1]);
                if (g2.puntos.contains(vecino)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Cuenta piezas totales de un jugador
     */
    private static int contarPiezas(GameStatus state, PlayerType player) {
        int count = 0;
        int size = state.getSquareSize();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Point p = new Point(i, j);
                if (state.isInBounds(p) && state.getColor(p) == player) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Obtiene todos los grupos de un jugador
     */
    private static List<Grupo> obtenerGrupos(GameStatus state, PlayerType player) {
        List<Grupo> grupos = new java.util.ArrayList<>();
        boolean[][] visitado = new boolean[state.getSquareSize()][state.getSquareSize()];
        
        for (int i = 0; i < state.getSquareSize(); i++) {
            for (int j = 0; j < state.getSquareSize(); j++) {
                Point p = new Point(i, j);
                if (state.isInBounds(p) && !visitado[i][j] && state.getColor(p) == player) {
                    Grupo grupo = new Grupo();
                    dfs(state, p, player, visitado, grupo);
                    grupos.add(grupo);
                }
            }
        }
        
        return grupos;
    }
    
    /**
     * DFS para construir un grupo
     */
    private static void dfs(GameStatus state, Point p, PlayerType player, boolean[][] visitado, Grupo grupo) {
        if (!state.isInBounds(p) || visitado[p.x][p.y] || state.getColor(p) != player) {
            return;
        }
        
        visitado[p.x][p.y] = true;
        grupo.puntos.add(new Point(p.x, p.y));
        grupo.tamano++;
        
        for (int[] dir : DIRECTIONS) {
            Point vecino = new Point(p.x + dir[0], p.y + dir[1]);
            dfs(state, vecino, player, visitado, grupo);
        }
    }
    
    /**
     * Clase para representar un grupo de piedras conectadas
     */
    private static class Grupo {
        Set<Point> puntos = new HashSet<>();
        int tamano = 0;
    }
}