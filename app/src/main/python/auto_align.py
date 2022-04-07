#Created by Petros Koutsouvelis in 03/2022
#The script performs automatic alignment of the trajectory.
#Firstly, the PDR trajectory based on the sensor measurement is computed, then
#the line equations are extracted using regression and are classified to the
#corresponding building region using EMF values. Aligned lines and complete
#trajectory are plotted and converted to byte array.

from os.path import dirname, join
from PIL import Image
import matplotlib
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import numpy as np
import io
import base64


def auto_align(sex, height, sep_plot):
    #Read function inputs:
    #----------------------------------------------------------------------------
    sex = int(sex)
    height = int(height)
    sep_plot = int(sep_plot) #0: plot complete trajectory
                             #1: plot each line separately

    #Read input files:
    #-----------------------------------------------------------------------------
    inputfile_1 = 'Sensor_20220324_164917_6405095473183875478.csv'
    inputfile_2 = 'Sensor_20220324_165108_1498070707813036265.csv'

    filename_1 = join(dirname(__file__), inputfile_1)
    inputfile_reader_1 = pd.read_csv(filename_1)
    filename_2 = join(dirname(__file__), inputfile_2)
    inputfile_reader_2 = pd.read_csv(filename_2) # Make each row a list with one string-type element
    inputfile_reader = pd.concat([inputfile_reader_1, inputfile_reader_2.iloc[1:]])
    inputfile_reader = inputfile_reader.reset_index()
    inputfile_reader = inputfile_reader.drop(columns = 'index')

    #Declare variables:
    #-----------------------------------------------------------------------------
    x_vals = []
    y_vals = []
    alpha = 0.9 #filter parameter
    threshold = -0.085 #step detection threshold
    m = 5 #smoothing factor
    min_ang = 10
    max_ang = 70
    step_detect = 0
    new_x = 0
    new_y = 0
    step_counter  = 0
    total_dist = 0
    prev_angle = 0
    total_dist_arr = []
    step_arr = []
    prev_x = 0
    prev_y = 0
    magh_vals = [] #for automatic alignment

    #Coordinate values of start-end points in all corridors
    lat_corners = [55.9230914, 55.9230448, 55.9225607, 55.9224297, 55.9224406,
                   55.9223084, 55.9227005, 55.922693, 55.9227185, 55.9226569]
    long_corners = [-3.17163, -3.1717641, -3.1712769, -3.1716507, -3.1717245,
                    -3.172093, -3.1725526, -3.1725745, -3.1726026, -3.1727689]

    #Label of each line in floor plan, from sanderson to fleeming jenkin buildings
    line = [0, 1, 2, 3, 4, 5, 6, 7, 8] #cannot skip lines

    #Thresholds for magnetic field strength in each line
    path1_vals = [43, 40, 33]
    path2_vals = [33, 37, 39, 40, 31, 34, 30, 28, 45]
    path3_vals = [47, 45, 42, 39]
    path4_vals = [40]
    path5_vals = [40, 39, 38, 41]
    path6_vals = [30, 28, 32, 26, 34, 33]
    path7_vals = [26]
    path8_vals = [26]
    path9_vals = [42, 27]

    path_vals = [path1_vals, path2_vals, path3_vals, path4_vals, path5_vals,
                 path6_vals, path7_vals, path8_vals, path9_vals]

    #Row indices for every 0.5 seconds (? to be changed), depending on the sampling rate
    #Assume sampling rate is 4 Hz
    sampling_rate = 4.8
    desired_rate = 2
    ratio = int(sampling_rate/desired_rate) #data to read every iteration


    #Set step length based on inputs
    if (sex):
        k = 0.415
    else:
        k = 0.413
    l = (k * height)/100 #to meters

    #Data pre-processing:
    #-----------------------------------------------------------------------------
    #Convert vector acceleration to scalar
    acc_arr = []

    for i in range(len(inputfile_reader)):
        scalar_acc = np.sqrt((inputfile_reader.iloc[i, 4]**2) + (inputfile_reader.iloc[i, 5]**2) +
                               (inputfile_reader.iloc[i, 4]**2))
        acc_arr.append(scalar_acc)

    #High-pass filter to remove gravity after isolating it with a low-pass filter
    acc_new = []
    ave_acc = np.mean(acc_arr)
    for i in range(len(inputfile_reader)):
        acc_pavg = acc_arr[i] * (1 - alpha) + ave_acc * alpha
        acc_new.append(acc_arr[i] - acc_pavg)


    #Perform smoothing as in paper "Fusion of WiFi, Smartphone Sensors
    #and Landmarks Using the Kalman Filter for Indoor Localization"

    a_m = [] #acceleration after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + acc_new[i + j]
        avg = sum_m/m
        a_m.append(avg)

    #correct angles, optimize code
    new_deg = []
    for i in range(len(inputfile_reader)):
        if (inputfile_reader.iloc[i,10] > 50):
            new_deg.append(inputfile_reader.iloc[i,10] - 360)
        else:
            new_deg.append(inputfile_reader.iloc[i,10])

    #Smoothing for degrees
    #degrees_z = inputfile_reader.iloc[:, 10].values #angles from rotation matrix
    degrees_z = new_deg
    d_m = [] #angle after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + degrees_z[i + j]
        avg = sum_m/m
        d_m.append(avg)

    #Calculate and plot trajectory
    #-------------------------------------------------------------------
    #Compute new coordinates
    for i in range(0, (len(a_m) - ratio + 1), ratio):
        avg_angle = np.mean(d_m[i : (i + ratio)])
        mag_val = np.mean(inputfile_reader.iloc[i : (i + ratio), 3]) #magnetic field strength for alignment
        for j in a_m[i : (i + ratio)]:
            if (j > threshold):
                step_detect = 1
        if (step_detect):
            if (abs(prev_angle - avg_angle) < min_ang): #Walk straight
                new_x = prev_x + l * np.sin(np.deg2rad(prev_angle))
                new_y = prev_y + l * np.cos(np.deg2rad(prev_angle))
            elif (abs(prev_angle - avg_angle) > min_ang) and (abs(prev_angle - avg_angle) < max_ang): #turn diagonally
                new_x = prev_x + l * np.sin(np.deg2rad(avg_angle))
                new_y = prev_y + l * np.cos(np.deg2rad(avg_angle))
                prev_angle = avg_angle
            else: #turn 90 degrees
                if (prev_angle - avg_angle) < 0:
                    avg_angle = prev_angle + 90
                    new_x = prev_x + l * np.sin(np.deg2rad(avg_angle))
                    new_y = prev_y + l * np.cos(np.deg2rad(avg_angle))
                    prev_angle = avg_angle
                else:
                    avg_angle = prev_angle - 90
                    new_x = prev_x + l * np.sin(np.deg2rad(avg_angle))
                    new_y = prev_y + l * np.cos(np.deg2rad(avg_angle))
                    prev_angle = avg_angle
            step_counter = step_counter + 1
            total_dist = total_dist + l
            prev_x = new_x
            prev_y = new_y
        x_vals.append(new_x)
        y_vals.append(new_y)
        step_arr.append(step_detect)
        total_dist_arr.append(total_dist)
        magh_vals.append(mag_val)
        step_detect = 0


    #Extract equations from trajectory using linear regression
    #-------------------------------------------------------------------
    Polynomial = np.polynomial.Polynomial

    m_sign = 0
    last_idx = 0
    m_vals = []
    b_vals = []
    y_intercepts = []
    ave_eq = []
    for i in range(len(x_vals) - 20):
        last_sign = m_sign
        longs = np.array(x_vals[i:i+20])
        lats = np.array(y_vals[i:i+20])
        cmin, cmax = min(longs), max(longs)
        pfit, stats = Polynomial.fit(longs, lats, 1, full=True, window=(cmin, cmax),
                                     domain=(cmin, cmax))
        A0, m = pfit

        #Processing
        y = 80*m + A0 #compute y-intercept for a near point, maximum distance possible from 0.
        y_intercepts.append(y)
        m_vals.append(m)
        b_vals.append(A0)

        if m < 0: m_sign = -1
        else: m_sign = 1
        if (last_sign != m_sign and i != 0) or (i == len(x_vals) - 21):
            ave_m = np.mean(m_vals[last_idx:i])
            ave_b = np.mean(b_vals[last_idx:i])
            ave_y = np.mean(y_intercepts[last_idx:i])
            if last_idx == 0:
                magh = np.mean(magh_vals[last_idx:i+15])
                dur = i  - last_idx
                steps = sum(step_arr[last_idx:i+15])
                ave_eq.append([ave_m, ave_b, dur, steps, magh, last_idx, i+15, ave_y])
                last_idx = i
            else:
                magh = np.mean(magh_vals[last_idx+15:i+15])
                dur = i  - last_idx
                steps = sum(step_arr[last_idx+15:i+15])
                ave_eq.append([ave_m, ave_b, dur, steps, magh, last_idx+15, i+15, ave_y])
                last_idx = i

    #Define functions to be used for automatic alignment
    #-------------------------------------------------------------------

    #function to convert meters to global coordinates
    def metres_to_deg(x_vals, y_vals, start_pos):
        start_long = start_pos[0]
        start_lat = start_pos[1]
        long_val = start_long
        lat_val = start_lat
        long_vals = []
        lat_vals = []
        latdeg_per_m = 1/111340.33010639016
        longdeg_per_m = 1/62521.56943020176
        for i in range(len(x_vals)-1):
            long_val = long_val + longdeg_per_m * (x_vals[i+1] - x_vals[i])
            lat_val = lat_val + latdeg_per_m * (y_vals[i+1] - y_vals[i])
            long_vals.append(long_val)
            lat_vals.append(lat_val)
        return long_vals, lat_vals

    #function to interpolate coordinate points between start and end coordinates of a segment
    def interpolate(long_corners, long_corners_next, lat_corners, lat_corners_next, ranges):
        long_interpolated = []
        lat_interpolated = []
        x = np.linspace(long_corners, long_corners_next, len(ranges)+1)
        y = np.linspace(lat_corners, lat_corners_next, len(ranges)+1)
        for i in range(len(x)):
            long_interpolated.append(x[i])
            lat_interpolated.append(y[i])
        return long_interpolated, lat_interpolated


    #Extract equations from trajectory using linear regression
    #-------------------------------------------------------------------
    #Plot floor plan as background
    image_name = 'map.png'
    image_file = join(dirname(__file__), image_name)
    img = mpimg.imread(image_file)
    fig = plt.figure()
    a1 = fig.add_subplot(1,1,1)
    a1.imshow(img, extent=[-3.17310, -3.1708, 55.92205, 55.92340])

    for i in range(len(ave_eq)):
        if ave_eq[i][0] > 0.1:
            #line = 0,2,4 or 8
            if ave_eq[i][7] > 50: #minimum possible y-intercept for line 0, even at
                                  #lowest starting point: exclude 2,4
                if ave_eq[i][4] < 39: #exclude 8
                    path = line[8]
                else:
                    path = line[0]
            else:
                if ave_eq[i][4] > 42.5:
                    path = line[2]
                else:
                    path = line[4]
        elif ave_eq[i][0] < -0.5:
            #line = 1,5 or 7
            if ave_eq[i][4] < 31:
                path = line[7]
            elif ave_eq[i][4] > 31 and ave_eq[i][4] < 35:
                path = line[5]
            else:
                path = line[1]
        elif ave_eq[i][0] > -0.5 and ave_eq[i][0] < -0.2:
            #only line 7 in case of inaccurate regression
            path = line[7]
        else:
            if ave_eq[i][4] > 45:
                path = line[3]
            else:
                path = line[6]

        #find starting point using magnetic field strength thresholds
        if i == 0:
            first_line = path
            start_magh = np.mean(magh_vals[:10])
            thresholds = path_vals[first_line]
            #Create equally spaced segments, each corresponding to a magnetic field threshold value
            long_interpolated, lat_interpolated = interpolate(long_corners[first_line], long_corners[first_line+1], \
                                                              lat_corners[first_line], lat_corners[first_line+1], thresholds)
            for j in range(len(thresholds)):
                if start_magh > thresholds[j]:
                    num = j
            start_long = long_interpolated[num]
            start_lat = lat_interpolated[num]
        #Plot
        if sep_plot:
            if i == 0:
                start_pos = [start_long, start_lat]
            else:
                start_pos = [long_corners[path], lat_corners[path]]
            long_vals, lat_vals = metres_to_deg(x_vals[ave_eq[i][5]:ave_eq[i][6]], \
                                                y_vals[ave_eq[i][5]:ave_eq[i][6]], start_pos)
            a1.plot(long_vals, lat_vals)

    if sep_plot == 0:
        start_pos = [start_long, start_lat]
        long_vals, lat_vals = metres_to_deg(x_vals, y_vals, start_pos)
        a1.plot(long_vals, lat_vals)

    #Use this canvas to convert image to numpy array
    fig.canvas.draw()
    img = np.fromstring(fig.canvas.tostring_rgb(), dtype = np.uint8, sep = '')
    img = img.reshape(fig.canvas.get_width_height()[::-1] + (3,))

    #Convert to PIL image and then to byte string to return it in the java code
    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff, format = 'PNG')
    img_str = base64.b64encode(buff.getvalue())
    out = "" + str(img_str, 'utf-8')
    
    return out

    













