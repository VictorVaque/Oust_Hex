package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.PlayerType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Heuristica {
    
    // PESOS
    private static final int PESO_PIEZA = 10;
    private static final int PESO_CAPTURA = 100;
    private static final int PESO_AMENAZA = -150;
    private static final int PESO_GRUPO = 20;
    
    // Direcciones hexagonales
    private static final int[][] DIRS = {
        {0, 1}, {1, 0}, {1, 1}, {0, -1}, {-1, 0}, {-1, -1}
    };
    
    public static int eval(GameStatus state, PlayerType player) {
        PlayerType opp = (player == PlayerType.PLAYER1) ? PlayerType.PLAYER2 : PlayerType.PLAYER1;
        
        int score = 0;
        int size = state.getSquareSize();
        boolean[][] visitado = new boolean[size][size];
        List<Grupo> misGrupos = new ArrayList<>();
        List<Grupo> gruposRival = new ArrayList<>();
        
        // Una sola pasada por el tablero
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Point p = new Point(i, j);
                if (!state.isInBounds(p) || visitado[i][j]) continue;
                
                PlayerType color = state.getColor(p);
                if (color == null) continue;
                
                // Construir grupo
                Grupo g = new Grupo();
                dfs(state, p, color, visitado, g);
                
                if (color == player) {
                    misGrupos.add(g);
                    score += g.tamano * PESO_PIEZA;
                    score += g.tamano * PESO_GRUPO;
                } else {
                    gruposRival.add(g);
                    score -= g.tamano * PESO_PIEZA;
                    score -= g.tamano * PESO_GRUPO;
                }
            }
        }
        
        // Analizar amenazas entre grupos en contacto
        for (Grupo mio : misGrupos) {
            for (Grupo rival : gruposRival) {
                if (contacto(mio, rival)) {
                    int dif = mio.tamano - rival.tamano;
                    if (dif > 0) {
                        score += rival.tamano * PESO_CAPTURA;
                    } else if (dif < 0) {
                        score += mio.tamano * PESO_AMENAZA;
                    }
                }
            }
        }
        
        return score;
    }
    
    private static void dfs(GameStatus state, Point p, PlayerType color, boolean[][] vis, Grupo g) {
        if (!state.isInBounds(p) || vis[p.x][p.y] || state.getColor(p) != color) return;
        
        vis[p.x][p.y] = true;
        g.puntos.add(p);
        g.tamano++;
        
        for (int[] d : DIRS) {
            dfs(state, new Point(p.x + d[0], p.y + d[1]), color, vis, g);
        }
    }
    
    private static boolean contacto(Grupo g1, Grupo g2) {
        for (Point p : g1.puntos) {
            for (int[] d : DIRS) {
                if (g2.puntos.contains(new Point(p.x + d[0], p.y + d[1]))) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private static class Grupo {
        Set<Point> puntos = new HashSet<>();
        int tamano = 0;
    }
}