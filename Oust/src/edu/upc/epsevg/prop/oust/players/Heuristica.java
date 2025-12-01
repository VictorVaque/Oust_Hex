package edu.upc.epsevg.prop.oust.players;

import edu.upc.epsevg.prop.oust.GameStatus;
import edu.upc.epsevg.prop.oust.PlayerType;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que implementa la función heurística para evaluar estados del juego Oust
 * Basada en el análisis de patrones hexagonales específicos
 */
public class Heuristica {
    
    // VALORES CONSTANTES PARA DIFERENTES PATRONES
    private static final int VALOR_4_EN_LINEA = 10000;
    private static final int VALOR_3_EN_LINEA = 1000;
    private static final int VALOR_2_EN_LINEA = 100;
    private static final int VALOR_1_EN_LINEA = 10;
    
    private static final int VALOR_BLOQUEO_3 = 30000;
    private static final int VALOR_BLOQUEO_2 = 500;
    
    private static final int VALOR_CENTRO = 5;
    private static final int VALOR_ESQUINA = 2;
    
    // Patrones de direcciones en un tablero hexagonal
    private static final int[][] DIRECTIONS = {
        {0, 1},   // Derecha
        {1, 0},   // Abajo-derecha
        {1, -1},  // Abajo-izquierda
        {0, -1},  // Izquierda
        {-1, 0},  // Arriba-izquierda
        {-1, 1}   // Arriba-derecha
    };
    
    /**
     * Evalúa un estado del juego desde la perspectiva del jugador maximizador
     */
    public static int eval(GameStatus state, PlayerType maximizingPlayer) {
        PlayerType opponent = (maximizingPlayer == PlayerType.PLAYER1) ? 
                             PlayerType.PLAYER2 : PlayerType.PLAYER1;
        
        int score = 0;
        
        // 1. Análisis de patrones lineales en todas las direcciones
        score += analizarPatronesLineales(state, maximizingPlayer, opponent);
        
        // 2. Valoración posicional del tablero
        score += analizarValorPosicional(state, maximizingPlayer, opponent);
        
        // 3. Movilidad y potencial de captura
        score += analizarMovilidad(state, maximizingPlayer, opponent);
        
        return score;
    }
    
    /**
     * Analiza patrones lineales en las 6 direcciones hexagonales
     */
    private static int analizarPatronesLineales(GameStatus state, PlayerType maximizingPlayer, PlayerType opponent) {
        int score = 0;
        int size = state.getSize();
        
        // Analizar cada casilla como punto de inicio de patrones
        for (int i = 0; i < size * 2 - 1; i++) {
            for (int j = 0; j < size * 2 - 1; j++) {
                Point p = new Point(i, j);
                if (state.isInBounds(p)) {
                    // Analizar en las 6 direcciones hexagonales
                    for (int[] dir : DIRECTIONS) {
                        score += evaluarPatronEnDireccion(state, p, dir, maximizingPlayer, opponent);
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Evalúa un patrón lineal en una dirección específica
     */
    private static int evaluarPatronEnDireccion(GameStatus state, Point start, int[] direction, 
                                               PlayerType maximizingPlayer, PlayerType opponent) {
        int score = 0;
        
        // Analizar ventanas de 4 casillas (como en Conecta 4)
        for (int length = 4; length >= 2; length--) {
            List<Point> ventana = obtenerVentana(state, start, direction, length);
            if (ventana != null) {
                int valorVentana = evaluarVentana(state, ventana, maximizingPlayer, opponent);
                score += valorVentana;
            }
        }
        
        return score;
    }
    
    /**
     * Obtiene una ventana de casillas en una dirección
     */
    private static List<Point> obtenerVentana(GameStatus state, Point start, int[] direction, int length) {
        List<Point> ventana = new ArrayList<>();
        
        for (int i = 0; i < length; i++) {
            int x = start.x + i * direction[0];
            int y = start.y + i * direction[1];
            Point p = new Point(x, y);
            
            if (!state.isInBounds(p)) {
                return null; // Ventana incompleta
            }
            ventana.add(p);
        }
        
        return ventana;
    }
    
    /**
     * Evalúa una ventana específica de casillas
     */
    private static int evaluarVentana(GameStatus state, List<Point> ventana, 
                                     PlayerType maximizingPlayer, PlayerType opponent) {
        int fichasPropias = 0;
        int fichasRival = 0;
        int vacios = 0;
        
        // Contar fichas en la ventana
        for (Point p : ventana) {
            PlayerType color = state.getColor(p);
            if (color == maximizingPlayer) {
                fichasPropias++;
            } else if (color == opponent) {
                fichasRival++;
            } else {
                vacios++;
            }
        }
        
        // Ventana bloqueada (ambos jugadores tienen fichas)
        if (fichasPropias > 0 && fichasRival > 0) {
            return 0;
        }
        
        int valor = 0;
        
        // Evaluar ventanas propias
        if (fichasPropias > 0) {
            switch (fichasPropias) {
                case 1:
                    valor = VALOR_1_EN_LINEA;
                    break;
                case 2:
                    valor = VALOR_2_EN_LINEA;
                    break;
                case 3:
                    valor = VALOR_3_EN_LINEA;
                    break;
                case 4:
                    valor = VALOR_4_EN_LINEA;
                    break;
            }
            
            // Bonus por vacíos jugables (potencial de completar)
            if (vacios > 0) {
                valor *= (1 + vacios);
            }
        }
        // Evaluar ventanas rivales (bloqueos)
        else if (fichasRival > 0) {
            switch (fichasRival) {
                case 2:
                    valor = -VALOR_BLOQUEO_2;
                    break;
                case 3:
                    valor = -VALOR_BLOQUEO_3;
                    break;
                case 4:
                    valor = -VALOR_4_EN_LINEA; // ¡Peligro!
                    break;
            }
            
            // Las amenazas con vacíos son más peligrosas
            if (vacios > 0) {
                valor *= (1 + vacios);
            }
        }
        
        return valor;
    }
    
    /**
     * Analiza el valor posicional de las casillas
     */
    private static int analizarValorPosicional(GameStatus state, PlayerType maximizingPlayer, PlayerType opponent) {
        int score = 0;
        int size = state.getSize();
        int centro = size - 1;
        
        for (int i = 0; i < size * 2 - 1; i++) {
            for (int j = 0; j < size * 2 - 1; j++) {
                Point p = new Point(i, j);
                if (state.isInBounds(p)) {
                    int valorCasilla = calcularValorCasilla(state, p, centro, maximizingPlayer, opponent);
                    score += valorCasilla;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Calcula el valor de una casilla específica basado en su posición
     */
    private static int calcularValorCasilla(GameStatus state, Point p, int centro, 
                                           PlayerType maximizingPlayer, PlayerType opponent) {
        int valor = 0;
        PlayerType color = state.getColor(p);
        
        if (color == null) {
            return 0; // Casilla vacía
        }
        
        // Calcular distancia al centro
        double distancia = Math.sqrt(Math.pow(p.x - centro, 2) + Math.pow(p.y - centro, 2));
        
        // Valor base por posición
        if (distancia < 2) {
            valor = VALOR_CENTRO; // Centro del tablero
        } else if (distancia > centro) {
            valor = VALOR_ESQUINA; // Esquinas
        } else {
            valor = 1; // Posición normal
        }
        
        // Aplicar el valor según el jugador
        if (color == maximizingPlayer) {
            return valor;
        } else {
            return -valor;
        }
    }
    
    /**
     * Analiza la movilidad y potencial de captura
     */
    private static int analizarMovilidad(GameStatus state, PlayerType maximizingPlayer, PlayerType opponent) {
        int score = 0;
        
        // Movilidad actual
        List<Point> misMovimientos = state.getMoves();
        int miMovilidad = misMovimientos.size();
        
        // Estimación de movilidad del oponente
        int movilidadOponente = 0;
        if (state.getCurrentPlayer() == opponent) {
            movilidadOponente = state.getMoves().size();
        }
        
        // Diferencia de movilidad
        score += (miMovilidad - movilidadOponente) * 5;
        
        // Potencial de captura - analizar movimientos que crean grupos grandes
        for (Point movimiento : misMovimientos) {
            GameStatus estadoTemp = new GameStatus(state);
            estadoTemp.placeStone(movimiento);
            
            // Valorar movimientos que crean grupos de 3 o más
            int tamanoGrupo = calcularTamanoGrupo(estadoTemp, movimiento, maximizingPlayer);
            if (tamanoGrupo >= 3) {
                score += tamanoGrupo * 10;
            }
        }
        
        return score;
    }
    
    /**
     * Calcula el tamaño del grupo después de un movimiento
     */
    private static int calcularTamanoGrupo(GameStatus state, Point movimiento, PlayerType player) {
        int tamano = 1; // Al menos la pieza colocada
        
        // Verificar piezas conectadas en las 6 direcciones
        for (int[] dir : DIRECTIONS) {
            Point vecino = new Point(movimiento.x + dir[0], movimiento.y + dir[1]);
            if (state.isInBounds(vecino) && state.getColor(vecino) == player) {
                tamano++;
            }
        }
        
        return tamano;
    }
}