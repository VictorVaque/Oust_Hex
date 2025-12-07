package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.PlayerType;
import java.awt.Point;
import java.util.HashSet;
import java.util.Set;

/**
 * Heurística
 */
public class Heuristica {
    
    private static final int[][] DIRS = {
        {0, 1}, {1, 0}, {1, 1}, {0, -1}, {-1, 0}, {-1, -1}
    };
    
    /**
     * Evaluació
     */
    public static int eval(GameStatus s, PlayerType p) {
        int score = 0;
        int size = s.getSquareSize();
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Point pt = new Point(i, j);
                if (!s.isInBounds(pt)) continue;
                
                PlayerType c = s.getColor(pt);
                if (c == null) continue;
                
                int val = 10; 
                

                int r1Propios = 0;
                int r1Rivales = 0;
                Set<Point> visitados = new HashSet<>();
                
                for (int[] d : DIRS) {
                    Point v1 = new Point(i + d[0], j + d[1]);
                    if (!s.isInBounds(v1)) continue;
                    visitados.add(v1);
                    
                    PlayerType cv1 = s.getColor(v1);
                    if (cv1 == c) {
                        r1Propios++;
                    } else if (cv1 != null) {
                        r1Rivales++;
                    }
                }
                
                int r2Propios = 0;
                int r2Rivales = 0;
                
                for (int[] d1 : DIRS) {
                    Point v1 = new Point(i + d1[0], j + d1[1]);
                    if (!s.isInBounds(v1)) continue;
                    
                    for (int[] d2 : DIRS) {
                        Point v2 = new Point(v1.x + d2[0], v1.y + d2[1]);
                        if (!s.isInBounds(v2) || visitados.contains(v2) || v2.equals(pt)) continue;
                        
                        visitados.add(v2);
                        PlayerType cv2 = s.getColor(v2);
                        
                        if (cv2 == c) {
                            r2Propios++;
                        } else if (cv2 != null) {
                            r2Rivales++;
                        }
                    }
                }
                
                // VALORACIÓN
                val += r1Propios * r1Propios * 3;  // Conexiones directas
                val -= r1Rivales * 15;              // Contacto rival
                val += r2Propios * 2;               // Soporte indirecto
                val -= r2Rivales * 3;               // Presión lejana
                
                score += (c == p) ? val : -val;
            }
        }
        
        return score;
    }
}