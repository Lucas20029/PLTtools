/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plttools.optimizer;

import java.awt.Point;
import plttools.PLTdata;

/**
 *
 * @author vlada
 */
public class CorrectorOptimizer extends AbstractOptimizer {

    @Override
    public PLTdata optimize() {
        int offsetX;
        int offsetY;
        PLTdata p = new PLTdata();
        p.setLineCount((int) (pd.getPocetCar()*1.5));

        if (settings.getCorrectorMoveToOrigin()) {
            offsetX = (int) (pd.getBoundingBox().getMinX() - settings.getCorrectorOffsetX());
            offsetY = (int) (pd.getBoundingBox().getMinY() - settings.getCorrectorOffsetY());
        } else {
            offsetX = 0;
            offsetY = 0;            
        }

        if (settings.getCorrectorMergeIdentic()) {
            float threshold = settings.getCorrectorTolerance();
            System.out.println("threshold = " + threshold);
            int l1_1, l1_2;
            int x1_1, y1_1, x1_2, y1_2;
            int l2_1, l2_2;
            int x2_1, y2_1, x2_2, y2_2;
            int i, j, k, l;
            boolean leaveOriginaLines = false;
            
            pd.calculateDistances();
            boolean lineProcessed[] = new boolean[pd.getPocetCar()];
            for(i=0; i<pd.getPocetCar();i++) {
                lineProcessed[i] = false;
            }
            
            // go through all lines
            for(i=0; i<pd.getPocetCar();i++) {
                l1_1 = pd.getLines_1()[i];
                l1_2 = pd.getLines_2()[i];
                x1_1 = pd.getPoint_x()[l1_1];
                y1_1 = pd.getPoint_y()[l1_1];
                x1_2 = pd.getPoint_x()[l1_2];
                y1_2 = pd.getPoint_y()[l1_2];
                float lengthOfLine_i = pd.getLengthOfLine(i);
                byte pen;
                int debugLine = 45;
                if (leaveOriginaLines) {
                    pen = 4;
                } else {
                    pen = pd.getPens()[i];
                }
                // skip already processed lines
                if (lineProcessed[i]) {
                        System.out.println("line #" + i + " skipped (conditions: "
                                + lineProcessed[i]+")");
                    continue;
                }
                
                
                // and find all lines that have at least portion in threshold distance and fill them into diplicitiesList
                int maxDuplicities = 10;
                Point midPoints[] = new Point[2*maxDuplicities];
                Point intersectPoints[] = new Point[2*maxDuplicities];
                int numDuplicities = 0;
                // pass through all lines, there might be shorter linex, that will be processed with longer lines later
                for(j=0; j<pd.getPocetCar(); j++) {
                    // skip already processed lines, identic lines, lines that are longer that line i and lines, that are part of contignuous line
                    l2_1 = pd.getLines_1()[j];
                    l2_2 = pd.getLines_2()[j];
                    if (lineProcessed[j] || (i==j) || (pd.getLengthOfLine(j) > lengthOfLine_i) || (pd.angleBetweenLines(i,j)>5)
                            || (l1_1 == l2_1) || (l1_1 == l2_2) || (l1_2 == l2_1) || (l1_2 == l2_2)) {
                        if (i==debugLine) {
                            System.out.println("line #"+i+" - line #" + j + " skipped (conditions: "
                                + lineProcessed[j] +", "
                                + (i==j) +", "
                                + (pd.getLengthOfLine(j) > lengthOfLine_i) +", "
                                + (pd.angleBetweenLines(i,j)>5) +", "     
                                + (l1_1 == l2_1) +", "
                                + (l1_1 == l2_2) +", "
                                + (l1_2 == l2_1) +", "
                                + (l1_2 == l2_2) +")");
                        }
                        continue;
                    }

                    x2_1 = pd.getPoint_x()[l2_1];
                    y2_1 = pd.getPoint_y()[l2_1];
                    x2_2 = pd.getPoint_x()[l2_2];
                    y2_2 = pd.getPoint_y()[l2_2];

                    // if the lines are in proximity, then two points must lie in shorter distance than threshold from opposite line
                    // we work only if both points of shorter line are close to longer line
                    // line i is the longer one; j is the shorter one
                    // (there are another possibilities - one point from longer line and one point from shorter line and all their combinations
                    float distShorter1_toLonger = pd.getDistanceOfPointFromLine(l2_1, i);
                    float distShorter2_toLonger = pd.getDistanceOfPointFromLine(l2_2, i);
                    if (i == debugLine) {
                        System.out.println("distance of line #"+j+", point #"+l2_1+" to line #"+i+" = "+distShorter1_toLonger);
                        System.out.println("distance of line #"+j+", point #"+l2_2+" to line #"+i+" = "+distShorter2_toLonger);
                        System.out.println("conditions: ("
                                + (distShorter1_toLonger < threshold) + ", "
                                + (distShorter2_toLonger < threshold) + ", "
                                + ((pd.getDistance(l2_1, l1_1) + pd.getDistance(l2_1, l1_2)) < (lengthOfLine_i + 2* threshold)) + ", "
                                + ((pd.getDistance(l2_2, l1_1) + pd.getDistance(l2_2, l1_2)) < (lengthOfLine_i + 2* threshold)) 
                                + ")");
                    }
                    
                    // if two points are within the distance, we have the duplicity of lines
                    // and we can calculate midpoints between line i and line j
                    if ( (distShorter1_toLonger < threshold) 
                            && (distShorter2_toLonger < threshold)
                            && (pd.getDistance(l2_1, l1_1) + pd.getDistance(l2_1, l1_2)) < (lengthOfLine_i + 2* threshold)
                            && (pd.getDistance(l2_2, l1_1) + pd.getDistance(l2_2, l1_2)) < (lengthOfLine_i + 2* threshold)
                            ) {
                        double distance_21_11 = pd.getDistance(l2_1, l1_1);
                        double distance_21_12 = pd.getDistance(l2_1, l1_2);
                        double distance_22_11 = pd.getDistance(l2_2, l1_1);
                        double distance_22_12 = pd.getDistance(l2_2, l1_2);
                        
                        System.out.println("line #"+i+" ["+x1_1+";"+x1_2+"] to ["+x1_2+";"+y1_2+"] added next two midpoints:");
                        if ((distance_21_11 < 2*threshold) && (distance_21_11 < distance_21_12)) {
                            intersectPoints[numDuplicities] = new Point(x1_1,y1_1);
                            midPoints[numDuplicities] = new Point( (int)((x1_1 + x2_1)/2), (int)((y1_1 + y2_1)/2));
                        } else if (distance_21_12 < 2*threshold) {
                            intersectPoints[numDuplicities] = new Point(x1_2,y1_2);
                            midPoints[numDuplicities] = new Point( (int)((x1_2 + x2_1)/2), (int)((y1_2 + y2_1)/2));
                        } else {
                            Point intersect1 = pd.getClosestPointOnLine(l2_1, i);
                            intersectPoints[numDuplicities] = intersect1;
                            midPoints[numDuplicities] = new Point( (int)((intersect1.getX() + x2_1)/2), (int)((intersect1.getY() + y2_1)/2));
                        }
                        System.out.println("\t\t\t ["+midPoints[numDuplicities]+"] for intersection ["+intersectPoints[numDuplicities]+"]");
                        numDuplicities++;                            
                        
                        if ((distance_22_11 < 2*threshold) && (distance_22_11 < distance_22_12)) {
                            intersectPoints[numDuplicities] = new Point(x1_1,y1_1);
                            midPoints[numDuplicities] = new Point( (int)((x1_1 + x2_2)/2), (int)((y1_1 + y2_2)/2));
                        } else if (distance_22_12 < 2*threshold) {
                            intersectPoints[numDuplicities] = new Point(x1_2,y1_2);
                            midPoints[numDuplicities] = new Point( (int)((x1_2 + x2_2)/2), (int)((y1_2 + y2_2)/2));
                        } else {
                            Point intersect2 = pd.getClosestPointOnLine(l2_2, i);
                            intersectPoints[numDuplicities] = intersect2;
                            midPoints[numDuplicities] = new Point( (int)((intersect2.getX() + x2_2)/2), (int)((intersect2.getY() + y2_2)/2));
                        }
                        System.out.println("\t\t\t ["+midPoints[numDuplicities]+"] for intersection ["+intersectPoints[numDuplicities]+"]");
                        numDuplicities++;                            

                        lineProcessed[j] = true;
                    }
                                  
                    if (numDuplicities >= maxDuplicities) {
                        System.out.println("maxDuplicities for line " + i + " reached!");
                        break;
                    }
                }
                
                // here we have duplicities, let's connect them, start from left bottom corner
                if (numDuplicities >0) {
                    // set the line i as having duplicities
                    lineProcessed[i] = true;
                    int lastX = -1;
                    int lastY = 0;   
                    int lastIntersecX = 0;
                    int lastIntersecY = 0;
                    int lastPoint;
                    int nextX, nextY;
                    // add first and last midpoint
                    
                    // and add another points into contignuous line
                    boolean pointUsed[] = new boolean[numDuplicities];
                    double distance;
                    boolean haveFirstPoint = false;
                    for(j=0; j<numDuplicities; j++) {
                        double minDistance = pd.getBoundingBox().getMaxX()+pd.getBoundingBox().getMaxY();

                        lastPoint = -1;
                        // first find the point which is next one in the line
                        System.out.print("midpoint: ");
                        for (k=0; k<(numDuplicities); k++) {
                            System.out.print(k+ " ... ");
                            // in next condition last point must be skipped
                            distance = calcDistance(lastIntersecX, lastIntersecY,(int) intersectPoints[k].getX(),(int) intersectPoints[k].getY());
                            if ((!pointUsed[k]) && (distance <= minDistance)) {
                                lastPoint = k;
                                minDistance = distance;                            
                            }
                        }
                        System.out.println();
                        System.out.println("last point = "+lastPoint);

                        // now we have next point
                        if (lastPoint >= 0) {
                            nextX = (int) midPoints[lastPoint].getX();
                            nextY = (int) midPoints[lastPoint].getY();
                            pointUsed[lastPoint] = true;
                            // add the line to optimized set of lines - but only if it is long enough and
                            if ((lastX >= 0)
                                    && (minDistance > 0.025 || (!haveFirstPoint && minDistance != 0))
                                    && !(lastX==nextX && lastY==nextY)) { // && pd.isPointInLine(intersectPoints[lastPoint], i)
                                System.out.println("line #"+i +": added duplicate line #" + j + " between ["+lastX+";"+lastY+"] and ["+nextX+";"+nextY+"]");
                                p.addLine(  lastX - offsetX,
                                            lastY - offsetY, 
                                            nextX - offsetX,
                                            nextY - offsetY, 
                                            pen); 
                                haveFirstPoint = true;
                            }
                            // and prepare variables for next run of finding next point on intersected line
                            lastX = nextX;
                            lastY = nextY;
                            lastIntersecX = (int) intersectPoints[lastPoint].getX();
                            lastIntersecY = (int) intersectPoints[lastPoint].getY();       
                        } else {
                            // break the loop if we reached the end of line ... meaning we have no line to add
                            break;
                        }
                    }
                }
                // and add the line to end X, if it wasn't added already
//                if (calcDistance(startX, startY,nextX,nextY) < lengthOfLine_i) {
//                    System.out.println("line #"+i +": added final line");
//                    p.addLine(  lastX - offsetX,
//                                lastY - offsetY, 
//                                endX - offsetX,
//                                endY - offsetY, 
//                                pd.getPens()[i]);
//                }

                
                // if the line does not have duplicities and is longer than threshold 
//                if (numDuplicities == 0) {
//                if ((numDuplicities == 0) && pd.calculateDistance(l1_1,l1_2) > threshold) {
                // set the line as processed not to return to them
//                    p.addLine(x1_1 - offsetX, y1_1- offsetY, x1_2- offsetX, y1_2- offsetY, pd.getPens()[i]);    
//                }

            }
            
            for(i=0; i<pd.getPocetCar();i++) {
                l1_1 = pd.getLines_1()[i];
                l1_2 = pd.getLines_2()[i];
                x1_1 = pd.getPoint_x()[l1_1];
                y1_1 = pd.getPoint_y()[l1_1];
                x1_2 = pd.getPoint_x()[l1_2];
                y1_2 = pd.getPoint_y()[l1_2];
                if (leaveOriginaLines || (! lineProcessed[i] && pd.calculateDistance(l1_1,l1_2) > threshold)) {
                    p.addLine(x1_1 - offsetX, y1_1- offsetY, x1_2- offsetX, y1_2- offsetY, pd.getPens()[i]);  
                }
            }
            p.calculateDistances();
            p.calculateStats();
            return p;
        } else if (settings.getCorrectorMoveToOrigin()) {
            System.out.println("offsets - X = "+offsetX+"; Y = " + offsetY );

            int x1, y1, x2, y2;
            for(int i=0; i< pd.getPocetCar(); i++) {
                x1 = pd.getPoint_x()[pd.getLines_1()[i]] - offsetX;
                y1 = pd.getPoint_y()[pd.getLines_1()[i]] - offsetY;
                x2 = pd.getPoint_x()[pd.getLines_2()[i]] - offsetX;
                y2 = pd.getPoint_y()[pd.getLines_2()[i]] - offsetY;
                p.addLine(x1, y1, x2, y2, pd.getPens()[i]);
//                System.out.println("added line ["+x1+";" + y1 + "] ["+ x2 + ";" + y2 +"]");
            }
            p.calculateDistances();
            p.calculateStats();
            return p;
        } else {
            return pd;
        }
    }
    
    private float calcDistance(int x1, int y1, int x2, int y2) {
        return (float) Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }
}